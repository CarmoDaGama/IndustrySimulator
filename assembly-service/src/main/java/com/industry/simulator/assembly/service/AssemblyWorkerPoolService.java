package com.industry.simulator.assembly.service;

import com.industry.simulator.assembly.entity.Inventory;
import com.industry.simulator.assembly.entity.PipelineStep;
import com.industry.simulator.assembly.entity.Product;
import com.industry.simulator.assembly.entity.ProductionRule;
import com.industry.simulator.assembly.entity.WorkerPoolConfig;
import com.industry.simulator.assembly.kafka.AssemblyProducer;
import com.industry.simulator.assembly.repository.InventoryRepository;
import com.industry.simulator.assembly.repository.PipelineStepRepository;
import com.industry.simulator.assembly.repository.ProductRepository;
import com.industry.simulator.assembly.repository.ProductionRuleRepository;
import com.industry.simulator.assembly.repository.WorkerPoolConfigRepository;
import com.industry.simulator.common.events.ComponentAssembledEvent;
import com.industry.simulator.common.events.InventoryUpdatedEvent;
import com.industry.simulator.common.events.ProductAssembledEvent;
import com.industry.simulator.common.model.Component;
import com.industry.simulator.common.worker.ProductionSpec;
import com.industry.simulator.common.worker.ProductionWorkerPool;
import com.industry.simulator.common.worker.StepSpec;
import com.industry.simulator.common.worker.WorkerActivity;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Camada 4 (Montagem). Aqui a regra de produção é, na prática, a <b>árvore
 * BOM</b> do produto: vários componentes distintos, cada um com a sua
 * quantidade (ex.: Motor ×1 + Pneus ×4 + Chassis ×1 → Carro ×1). O Worker só
 * arranca quando <b>todos</b> os componentes exigidos estiverem disponíveis.
 */
@Service
public class AssemblyWorkerPoolService {

    private static final Logger log = LoggerFactory.getLogger(AssemblyWorkerPoolService.class);
    private static final String SERVICE = "assembly-service";
    private static final String DEFAULT_FACTORY = "central-assembly-line-delta";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AssemblyProducer producer;

    @Autowired
    private PipelineStepRepository pipelineRepository;

    @Autowired
    private ProductionRuleRepository ruleRepository;

    @Autowired
    private WorkerPoolConfigRepository workerPoolConfigRepository;

    private ProductionWorkerPool<ComponentAssembledEvent, ProductAssembledEvent> pool;

    @PostConstruct
    public void init() {
        pool = new ProductionWorkerPool<>(
                "assembly",
                this::currentSpecs,
                this::currentSteps,
                event -> event.getPayload().getName(),
                event -> event.getPayload().getBatchId(),
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
    public Map<String, Integer> getQueueByMaterial() { return pool.getQueueByMaterial(); }
    public List<WorkerActivity> getActivities() { return pool.getActivities(); }

    /** Produtos que a fábrica sabe montar — alimenta a simulação de clientes. */
    public List<String> getProducibleProducts() {
        return ruleRepository.findByActiveTrue().stream()
                .map(ProductionRule::getOutputMaterial)
                .distinct()
                .collect(Collectors.toList());
    }

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

    private List<ProductionSpec> currentSpecs() {
        return ruleRepository.findByActiveTrue().stream().map(this::toSpec).collect(Collectors.toList());
    }

    private ProductionSpec toSpec(ProductionRule rule) {
        List<ProductionSpec.Input> inputs = rule.getInputs().stream()
                .map(i -> new ProductionSpec.Input(i.getInputMaterial(), i.getInputQuantity()))
                .collect(Collectors.toList());
        return new ProductionSpec(
                rule.getOutputMaterial(),
                rule.getOutputType() != null ? rule.getOutputType() : "FINAL_PRODUCT",
                rule.getOutputQuantity(),
                rule.getFactory() != null ? rule.getFactory() : DEFAULT_FACTORY,
                rule.getTargetProduct(), rule.getTargetComponent(), rule.getDescription(),
                inputs
        );
    }

    private List<StepSpec> currentSteps() {
        List<PipelineStep> steps = pipelineRepository.findAllByIsActiveOrderByStepOrderAsc(true);
        return steps.stream()
                .map(s -> new StepSpec(s.getStepName(), s.getDurationMs()))
                .collect(Collectors.toList());
    }

    private ProductAssembledEvent produce(ProductionSpec spec, List<ComponentAssembledEvent> consumed) {
        String productId = UUID.randomUUID().toString();
        String sourceBatchIds = consumed.stream()
                .map(e -> e.getPayload().getBatchId())
                .collect(Collectors.joining(","));

        log.info("{} | {} | BOM de '{}' satisfeita: {} componente(s) consumidos",
                productId, SERVICE, spec.getOutputMaterial(), consumed.size());

        Product productEntity = new Product();
        productEntity.setProductId(productId);
        productEntity.setProductName(spec.getOutputMaterial());
        productEntity.setBatchId(productId);
        productEntity.setSourceBatchIds(sourceBatchIds);
        productEntity.setComponentCount(consumed.size());
        productEntity.setAssembled(true);
        productEntity.setStatus("COMPLETED");
        productEntity.setCreatedAt(LocalDateTime.now());
        productEntity.setAssembledAt(LocalDateTime.now());
        productRepository.save(productEntity);

        Component payload = Component.builder()
                .id(productId)
                .name(spec.getOutputMaterial())
                .type(spec.getOutputType())
                .quantity(1.0)
                .unit("unit")
                .batchId(productId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .producer(Component.Producer.builder()
                        .service(SERVICE)
                        .factory(spec.getFactory())
                        .build())
                .purpose(Component.Purpose.builder()
                        .targetProduct(spec.getTargetProduct())
                        .targetComponent(spec.getTargetComponent())
                        .description(spec.getDescription())
                        .build())
                .components(consumed.stream()
                        .map(ComponentAssembledEvent::getPayload)
                        .collect(Collectors.toList()))
                .build();

        return ProductAssembledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PRODUCT_ASSEMBLED")
                .timestamp(System.currentTimeMillis() / 1000)
                .payload(payload)
                .build();
    }

    private void publishAndUpdateInventory(ProductAssembledEvent productEvent) {
        producer.publishProductAssembled(productEvent);

        String productName = productEvent.getPayload().getName();
        List<Inventory> existing = inventoryRepository.findByProductName(productName);
        Inventory inventory = existing.isEmpty() ? new Inventory() : existing.get(0);
        double quantityBefore = inventory.getQuantity();

        if (existing.isEmpty()) {
            inventory.setProductId(productEvent.getPayload().getId());
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

        producer.publishInventoryUpdated(InventoryUpdatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("INVENTORY_UPDATED")
                .timestamp(System.currentTimeMillis() / 1000)
                .payload(InventoryUpdatedEvent.Payload.builder()
                        .id(productEvent.getPayload().getId())
                        .name(productName)
                        .quantityBefore(quantityBefore)
                        .quantityAfter(inventory.getQuantity())
                        .operation("ADD")
                        .reason("product_assembly")
                        .build())
                .build());
    }

    private void handleError(Exception ex, List<ComponentAssembledEvent> consumed) {
        for (ComponentAssembledEvent event : consumed) {
            try {
                Product errorProduct = new Product();
                errorProduct.setProductId(UUID.randomUUID().toString());
                errorProduct.setProductName("FALHA-" + event.getPayload().getName());
                errorProduct.setBatchId(event.getPayload().getBatchId());
                errorProduct.setSourceBatchIds(event.getPayload().getBatchId());
                errorProduct.setComponentCount(1);
                errorProduct.setAssembled(false);
                errorProduct.setStatus("FAILED");
                errorProduct.setErrorMessage(ex.getMessage());
                errorProduct.setCreatedAt(LocalDateTime.now());
                productRepository.save(errorProduct);
            } catch (Exception persistError) {
                log.error("{} | {} | Erro ao gravar falha", event.getEventId(), SERVICE, persistError);
            }
        }
    }
}
