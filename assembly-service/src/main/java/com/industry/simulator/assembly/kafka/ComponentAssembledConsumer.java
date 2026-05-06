package com.industry.simulator.assembly.kafka;

import com.industry.simulator.common.events.ComponentAssembledEvent;
import com.industry.simulator.common.events.ProductAssembledEvent;
import com.industry.simulator.assembly.entity.Product;
import com.industry.simulator.assembly.entity.Inventory;
import com.industry.simulator.assembly.repository.ProductRepository;
import com.industry.simulator.assembly.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ComponentAssembledConsumer {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AssemblyProducer producer;

    @KafkaListener(topics = "component-assembled", groupId = "assembly-group")
    public void consumeComponentAssembled(ComponentAssembledEvent event) {
        log.info("Assembly service received component assembled event: {}", event.getBatchId());

        try {
            // Simulate async product assembly
            CompletableFuture.runAsync(() -> {
                try {
                    long assemblyDuration = 4000; // 4 seconds
                    log.info("Starting product assembly for batch {}", event.getBatchId());
                    Thread.sleep(assemblyDuration);

                    // Create product in database
                    String productId = UUID.randomUUID().toString();
                    Product product = Product.builder()
                            .productId(productId)
                            .productName("Car_" + event.getBatchId())
                            .batchId(event.getBatchId())
                            .componentCount(1)
                            .assembled(true)
                            .status("COMPLETED")
                            .errorMessage(null)
                            .createdAt(LocalDateTime.now())
                            .assembledAt(LocalDateTime.now())
                            .build();

                    productRepository.save(product);
                    log.info("Product assembled: {}", productId);

                    // Update inventory
                    String productName = "Car_" + event.getBatchId();
                    List<Inventory> existingList = inventoryRepository.findByProductName(productName);
                    Inventory inventory = existingList.isEmpty()
                            ? Inventory.builder()
                                    .productId(productId)
                                    .productName(productName)
                                    .quantity(0)
                                    .reservedQuantity(0)
                                    .availableQuantity(0)
                                    .location("Warehouse")
                                    .lastUpdated(LocalDateTime.now())
                                    .build()
                            : existingList.get(0);

                    inventory.setQuantity(inventory.getQuantity() + 1);
                    inventory.setAvailableQuantity(inventory.getQuantity() - inventory.getReservedQuantity());
                    inventory.setLastUpdated(LocalDateTime.now());
                    inventoryRepository.save(inventory);

                    // Publish product assembled event
                    ProductAssembledEvent productEvent = ProductAssembledEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .productId(productId)
                            .batchId(event.getBatchId())
                            .timestamp(LocalDateTime.now())
                            .purpose(event.getPurpose())
                            .success(true)
                            .errorMessage(null)
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
                    log.error("Assembly interrupted for batch {}", event.getBatchId(), e);
                }
            });

        } catch (Exception e) {
            log.error("Error processing component assembled event", e);
        }
    }
}
