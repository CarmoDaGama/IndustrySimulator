package com.industry.simulator.assembly.service;

import com.industry.simulator.assembly.entity.Inventory;
import com.industry.simulator.assembly.entity.PipelineStep;
import com.industry.simulator.assembly.entity.Product;
import com.industry.simulator.assembly.entity.WorkerPoolConfig;
import com.industry.simulator.assembly.kafka.AssemblyProducer;
import com.industry.simulator.assembly.repository.InventoryRepository;
import com.industry.simulator.assembly.repository.PipelineStepRepository;
import com.industry.simulator.assembly.repository.ProductRepository;
import com.industry.simulator.assembly.repository.WorkerPoolConfigRepository;
import com.industry.simulator.common.events.ComponentAssembledEvent;
import com.industry.simulator.common.events.InventoryUpdatedEvent;
import com.industry.simulator.common.events.ProductAssembledEvent;
import com.industry.simulator.common.worker.TwoToOneWorkerPool;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Camada de Montagem (Camada 4). Cumpre a regra 2:1: consome 2 componentes
 * para produzir 1 produto final, actualiza o Inventário (Camada 5) e
 * publica os eventos correspondentes.
 */
@Service
public class AssemblyWorkerPoolService {

    private static final Logger log = LoggerFactory.getLogger(AssemblyWorkerPoolService.class);
    private static final int INPUTS_PER_OUTPUT = 2;
    private static final long DEFAULT_DURATION_MS = 4000L;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AssemblyProducer producer;

    @Autowired
    private PipelineStepRepository pipelineRepository;

    @Autowired
    private WorkerPoolConfigRepository workerPoolConfigRepository;

    private TwoToOneWorkerPool<ComponentAssembledEvent, ProductAssembledEvent> pool;

    @PostConstruct
    public void init() {
        pool = new TwoToOneWorkerPool<>(
                "assembly",
                INPUTS_PER_OUTPUT,
                this::currentDurationMs,
                this::produce,
                this::publishAndUpdateInventory,
                this::handleError
        );
        pool.resize(currentWorkerCount());
    }

    public void submit(ComponentAssembledEvent event) {
        pool.submit(event);
    }

    public int getWorkerCount() { return pool.getWorkerCount(); }
    public int getQueueSize() { return pool.getQueueSize(); }

    public synchronized int resize(int workerCount) {
        WorkerPoolConfig config = workerPoolConfigRepository.findById(1L).orElseGet(WorkerPoolConfig::new);
        config.setWorkerCount(workerCount);
        workerPoolConfigRepository.save(config);
        pool.resize(workerCount);
        return pool.getWorkerCount();
    }

    private int currentWorkerCount() {
        return workerPoolConfigRepository.findById(1L)
                .map(WorkerPoolConfig::getWorkerCount)
                .orElseGet(() -> {
                    WorkerPoolConfig config = new WorkerPoolConfig();
                    workerPoolConfigRepository.save(config);
                    return config.getWorkerCount();
                });
    }

    private long currentDurationMs() {
        List<PipelineStep> steps = pipelineRepository.findAllByIsActiveOrderByStepOrderAsc(true);
        long total = steps.stream().mapToLong(PipelineStep::getDurationMs).sum();
        return total > 0 ? total : DEFAULT_DURATION_MS;
    }

    private ProductAssembledEvent produce(List<ComponentAssembledEvent> batch) {
        String productId = UUID.randomUUID().toString();
        String sourceBatchIds = batch.stream().map(ComponentAssembledEvent::getBatchId)
                .collect(Collectors.joining(","));
        ComponentAssembledEvent first = batch.get(0);
        long durationMs = currentDurationMs();

        log.info("{} | assembly-service | Consumindo {} componentes ({}) para produzir 1 produto final",
                productId, batch.size(), sourceBatchIds);

        // Nome do produto derivado do componente de origem (genérico, não hardcoded)
        String productName = "Product-" + first.getFinalComponent().getName();

        Product productEntity = new Product();
        productEntity.setProductId(productId);
        productEntity.setProductName(productName);
        productEntity.setBatchId(productId);
        productEntity.setSourceBatchIds(sourceBatchIds);
        productEntity.setComponentCount(batch.size());
        productEntity.setAssembled(true);
        productEntity.setStatus("COMPLETED");
        productEntity.setCreatedAt(LocalDateTime.now());
        productEntity.setAssembledAt(LocalDateTime.now());
        productRepository.save(productEntity);

        com.industry.simulator.common.model.Component finalProduct = com.industry.simulator.common.model.Component.builder()
                .id(productId)
                .name(productName)
                .type("FINAL_PRODUCT")
                .quantity(1.0)
                .unit("unit")
                .batchId(productId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .producer(com.industry.simulator.common.model.Component.Producer.builder()
                        .service("assembly-service")
                        .factory("central-assembly-line-delta")
                        .build())
                .purpose(first.getFinalComponent().getPurpose())
                .components(batch.stream().map(ComponentAssembledEvent::getFinalComponent).collect(Collectors.toList()))
                .build();

        return ProductAssembledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PRODUCT_ASSEMBLED")
                .productId(productId)
                .batchId(productId)
                .finalProduct(finalProduct)
                .assemblyDurationMs(durationMs)
                .timestamp(LocalDateTime.now())
                .purpose(first.getPurpose())
                .success(true)
                .build();
    }

    private void publishAndUpdateInventory(ProductAssembledEvent productEvent) {
        producer.publishProductAssembled(productEvent);

        String productName = productEvent.getFinalProduct().getName();
        List<Inventory> existingList = inventoryRepository.findByProductName(productName);
        Inventory inventory = existingList.isEmpty() ? new Inventory() : existingList.get(0);
        double quantityBefore = inventory.getQuantity();

        if (existingList.isEmpty()) {
            inventory.setProductId(productEvent.getProductId());
            inventory.setProductName(productName);
            inventory.setQuantity(0);
            inventory.setReservedQuantity(0);
            inventory.setAvailableQuantity(0);
            inventory.setLocation("Warehouse");
        }

        inventory.setQuantity(inventory.getQuantity() + 1);
        inventory.setAvailableQuantity(inventory.getQuantity() - inventory.getReservedQuantity());
        inventory.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(inventory);

        InventoryUpdatedEvent inventoryEvent = InventoryUpdatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .componentId(productEvent.getProductId())
                .componentName(productName)
                .quantityBefore(quantityBefore)
                .quantityAfter(inventory.getQuantity())
                .operation("ADD")
                .timestamp(LocalDateTime.now())
                .purpose(productEvent.getPurpose())
                .reason("product_assembly")
                .build();

        producer.publishInventoryUpdated(inventoryEvent);
    }

    private void handleError(Exception ex, List<ComponentAssembledEvent> batch) {
        for (ComponentAssembledEvent event : batch) {
            try {
                Product errorProduct = new Product();
                errorProduct.setProductId(UUID.randomUUID().toString());
                errorProduct.setProductName("FAILED-" + event.getBatchId());
                errorProduct.setBatchId(event.getBatchId());
                errorProduct.setSourceBatchIds(event.getBatchId());
                errorProduct.setComponentCount(1);
                errorProduct.setAssembled(false);
                errorProduct.setStatus("FAILED");
                errorProduct.setErrorMessage(ex.getMessage());
                errorProduct.setCreatedAt(LocalDateTime.now());
                productRepository.save(errorProduct);
            } catch (Exception persistError) {
                log.error("{} | assembly-service | Erro ao gravar falha de assembly", event.getBatchId(), persistError);
            }
        }
    }
}
