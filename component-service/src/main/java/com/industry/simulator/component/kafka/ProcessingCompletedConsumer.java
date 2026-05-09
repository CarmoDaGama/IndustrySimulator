package com.industry.simulator.component.kafka;

import com.industry.simulator.common.events.ProcessingCompletedEvent;
import com.industry.simulator.common.events.ComponentAssembledEvent;
import com.industry.simulator.component.entity.Component;
import com.industry.simulator.component.repository.ComponentRepository;
import com.industry.simulator.component.service.BOMValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ProcessingCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProcessingCompletedConsumer.class);

    @Autowired
    private ComponentRepository repository;

    @Autowired
    private ComponentProducer producer;

    @Autowired
    private BOMValidationService bomValidationService;

    @KafkaListener(topics = "processing-completed", groupId = "component-group")
    public void consumeProcessingCompleted(ProcessingCompletedEvent event) {
        String batchId = event.getBatchId();
        log.info("{} | component-service | Received processing completed event", batchId);

        try {
            // Simulate async component assembly with validation
            CompletableFuture.runAsync(() -> {
                try {
                    // Simulate assembly time
                    long assemblyDuration = 3000; // 3 seconds
                    log.info("{} | component-service | Starting component assembly ({}ms)", batchId, assemblyDuration);
                    Thread.sleep(assemblyDuration);

                    // Validate BOM compatibility
                    boolean compatible = bomValidationService.validateComponentCompatibility(
                            event.getProcessedMaterial().getType()
                    );

                    String compatibilityNotes = compatible 
                            ? "Material compatible with BOM requirements"
                            : "Material may have compatibility issues with BOM";

                    // Save component to database
                    Component entity = new Component();
                    entity.setBatchId(batchId);
                    entity.setComponentName(event.getProcessedMaterial().getName());
                    entity.setComponentType(event.getProcessedMaterial().getType());
                    entity.setQuantity(1.0);
                    entity.setUnit("unit");
                    entity.setProcessingType(event.getProcessingType());
                    entity.setBomValidated(true);
                    entity.setCompatible(compatible);
                    entity.setCompatibilityNotes(compatibilityNotes);
                    entity.setPurpose(event.getPurpose());
                    entity.setAssembled(true);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setAssembledAt(LocalDateTime.now());

                    repository.save(entity);
                    log.info("{} | component-service | Component assembled", batchId);

                    // v2 Requirement: Build recursive tree
                    com.industry.simulator.common.model.Component finalPart = com.industry.simulator.common.model.Component.builder()
                            .id("comp-" + UUID.randomUUID().toString().substring(0, 8))
                            .name("Industrial Part: " + event.getProcessedMaterial().getName())
                            .type("COMPONENT")
                            .quantity(1.0)
                            .unit("unit")
                            .batchId(batchId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .producer(com.industry.simulator.common.model.Component.Producer.builder()
                                    .service("component-service")
                                    .factory("manufacturing-hub-gamma")
                                    .build())
                            .compatibleForAssembly(compatible)
                            .components(Collections.singletonList(event.getProcessedMaterial()))
                            .build();

                    // Publish component assembled event
                    ComponentAssembledEvent assembledEvent = ComponentAssembledEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("COMPONENT_CREATED")
                            .batchId(batchId)
                            .finalComponent(finalPart)
                            .timestamp(LocalDateTime.now())
                            .purpose(event.getPurpose())
                            .success(true)
                            .build();

                    producer.publishComponentAssembled(assembledEvent);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("{} | component-service | Assembly interrupted", batchId, e);
                    handleAssemblyError(event);
                }
            });

        } catch (Exception e) {
            log.error("{} | component-service | Error processing completion event", batchId, e);
            handleAssemblyError(event);
        }
    }

    private void handleAssemblyError(ProcessingCompletedEvent event) {
        String batchId = event.getBatchId();
        try {
            Component errorComponent = new Component();
            errorComponent.setBatchId(batchId);
            errorComponent.setComponentName(event.getProcessedMaterial().getName());
            errorComponent.setComponentType(event.getProcessedMaterial().getType());
            errorComponent.setQuantity(1.0);
            errorComponent.setUnit("unit");
            errorComponent.setProcessingType(event.getProcessingType());
            errorComponent.setBomValidated(false);
            errorComponent.setCompatible(false);
            errorComponent.setCompatibilityNotes("Assembly failed");
            errorComponent.setPurpose(event.getPurpose());
            errorComponent.setAssembled(false);
            errorComponent.setCreatedAt(LocalDateTime.now());
            errorComponent.setAssembledAt(LocalDateTime.now());

            repository.save(errorComponent);
        } catch (Exception e) {
            log.error("{} | component-service | Error saving failed assembly record", batchId, e);
        }
    }
}
