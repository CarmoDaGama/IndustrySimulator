package com.industry.simulator.component.kafka;

import com.industry.simulator.common.events.ProcessingCompletedEvent;
import com.industry.simulator.common.events.ComponentAssembledEvent;
import com.industry.simulator.component.entity.Component;
import com.industry.simulator.component.repository.ComponentRepository;
import com.industry.simulator.component.service.BOMValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class ProcessingCompletedConsumer {

    @Autowired
    private ComponentRepository repository;

    @Autowired
    private ComponentProducer producer;

    @Autowired
    private BOMValidationService bomValidationService;

    @KafkaListener(topics = "processing-completed", groupId = "component-group")
    public void consumeProcessingCompleted(ProcessingCompletedEvent event) {
        log.info("Component service received processing completed event: {}", event.getBatchId());

        try {
            // Simulate async component assembly with validation
            CompletableFuture.runAsync(() -> {
                try {
                    // Simulate assembly time
                    long assemblyDuration = 3000; // 3 seconds
                    log.info("Starting component assembly for batch {}", event.getBatchId());
                    Thread.sleep(assemblyDuration);

                    // Validate BOM compatibility
                    boolean compatible = bomValidationService.validateComponentCompatibility(
                            event.getProcessedMaterial().getType()
                    );

                    String compatibilityNotes = compatible 
                            ? "Material compatible with BOM requirements"
                            : "Material may have compatibility issues with BOM";

                    // Save component to database
                    Component component = Component.builder()
                            .batchId(event.getBatchId())
                            .componentName(event.getProcessedMaterial().getName())
                            .componentType(event.getProcessedMaterial().getType())
                            .quantity(1.0)
                            .unit("unit")
                            .processingType(event.getProcessingType())
                            .bomValidated(true)
                            .compatible(compatible)
                            .compatibilityNotes(compatibilityNotes)
                            .purpose(event.getPurpose())
                            .assembled(true)
                            .createdAt(LocalDateTime.now())
                            .assembledAt(LocalDateTime.now())
                            .build();

                    repository.save(component);
                    log.info("Component assembled for batch {}", event.getBatchId());

                    // Publish component assembled event
                    ComponentAssembledEvent assembledEvent = ComponentAssembledEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .batchId(event.getBatchId())
                            .finalComponent(event.getProcessedMaterial())
                            .timestamp(LocalDateTime.now())
                            .purpose(event.getPurpose())
                            .success(true)
                            .errorMessage(null)
                            .build();

                    producer.publishComponentAssembled(assembledEvent);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Assembly interrupted for batch {}", event.getBatchId(), e);
                    handleAssemblyError(event);
                }
            });

        } catch (Exception e) {
            log.error("Error processing completion event", e);
            handleAssemblyError(event);
        }
    }

    private void handleAssemblyError(ProcessingCompletedEvent event) {
        try {
            Component errorComponent = Component.builder()
                    .batchId(event.getBatchId())
                    .componentName(event.getProcessedMaterial().getName())
                    .componentType(event.getProcessedMaterial().getType())
                    .quantity(1.0)
                    .unit("unit")
                    .processingType(event.getProcessingType())
                    .bomValidated(false)
                    .compatible(false)
                    .compatibilityNotes("Assembly failed")
                    .purpose(event.getPurpose())
                    .assembled(false)
                    .createdAt(LocalDateTime.now())
                    .assembledAt(LocalDateTime.now())
                    .build();

            repository.save(errorComponent);
        } catch (Exception e) {
            log.error("Error saving failed assembly record", e);
        }
    }
}
