package com.industry.simulator.assembly.kafka;

import com.industry.simulator.common.events.ComponentAssembledEvent;
import com.industry.simulator.common.events.ProductAssembledEvent;
import com.industry.simulator.assembly.entity.Product;
import com.industry.simulator.assembly.entity.Inventory;
import com.industry.simulator.assembly.repository.ProductRepository;
import com.industry.simulator.assembly.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ComponentAssembledConsumer {

    private static final Logger log = LoggerFactory.getLogger(ComponentAssembledConsumer.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AssemblyProducer producer;

    @KafkaListener(topics = "component-assembled", groupId = "assembly-group")
    public void consumeComponentAssembled(ComponentAssembledEvent event) {
        String batchId = event.getBatchId();
        log.info("{} | assembly-service | Received component assembled event", batchId);

        try {
            // Simulate async product assembly
            CompletableFuture.runAsync(() -> {
                try {
                    long assemblyDuration = 4000; // 4 seconds
                    log.info("{} | assembly-service | Starting product assembly ({}ms)", batchId, assemblyDuration);
                    Thread.sleep(assemblyDuration);

                    // Create product in database
                    String productId = UUID.randomUUID().toString();
                    Product productEntity = new Product();
                    productEntity.setProductId(productId);
                    productEntity.setProductName("Car_" + batchId);
                    productEntity.setBatchId(batchId);
                    productEntity.setComponentCount(1);
                    productEntity.setAssembled(true);
                    productEntity.setStatus("COMPLETED");
                    productEntity.setCreatedAt(LocalDateTime.now());
                    productEntity.setAssembledAt(LocalDateTime.now());

                    productRepository.save(productEntity);
                    log.info("{} | assembly-service | Product assembled: {}", batchId, productId);

                    // Update inventory
                    String productName = "Car_" + batchId;
                    List<Inventory> existingList = inventoryRepository.findByProductName(productName);
                    Inventory inventory = existingList.isEmpty()
                            ? new Inventory()
                            : existingList.get(0);
                    
                    if (existingList.isEmpty()) {
                        inventory.setProductId(productId);
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

                    // v2 Requirement: Build recursive tree
                    com.industry.simulator.common.model.Component finalProduct = com.industry.simulator.common.model.Component.builder()
                            .id(productId)
                            .name(productName)
                            .type("FINAL_PRODUCT")
                            .quantity(1.0)
                            .unit("unit")
                            .batchId(batchId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .producer(com.industry.simulator.common.model.Component.Producer.builder()
                                    .service("assembly-service")
                                    .factory("central-assembly-line-delta")
                                    .build())
                            .components(Collections.singletonList(event.getFinalComponent()))
                            .build();

                    // Publish product assembled event
                    ProductAssembledEvent productEvent = ProductAssembledEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("PRODUCT_ASSEMBLED")
                            .productId(productId)
                            .batchId(batchId)
                            .finalProduct(finalProduct)
                            .timestamp(LocalDateTime.now())
                            .purpose(event.getPurpose())
                            .success(true)
                            .build();

                    producer.publishProductAssembled(productEvent);

                    // Publish inventory updated event
                    com.industry.simulator.common.events.InventoryUpdatedEvent inventoryEvent = 
                            com.industry.simulator.common.events.InventoryUpdatedEvent.builder()
                                    .eventId(UUID.randomUUID().toString())
                                    .componentId(productId)
                                    .componentName(productName)
                                    .quantityBefore(inventory.getQuantity() - 1)
                                    .quantityAfter(inventory.getQuantity())
                                    .operation("ADD")
                                    .timestamp(LocalDateTime.now())
                                    .purpose(event.getPurpose())
                                    .reason("product_assembly")
                                    .build();

                    producer.publishInventoryUpdated(inventoryEvent);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("{} | assembly-service | Assembly interrupted", batchId, e);
                }
            });

        } catch (Exception e) {
            log.error("{} | assembly-service | Error processing component assembled event", batchId, e);
        }
    }
}
