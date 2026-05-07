package com.industry.simulator.processing.kafka;

import com.industry.simulator.common.events.RawMaterialProducedEvent;
import com.industry.simulator.common.events.ProcessingCompletedEvent;
import com.industry.simulator.processing.entity.ProcessedMaterial;
import com.industry.simulator.processing.repository.ProcessedMaterialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class RawMaterialConsumer {

    @Autowired
    private ProcessedMaterialRepository repository;

    @Autowired
    private ProcessingProducer producer;

    @Autowired
    private com.industry.simulator.processing.repository.PipelineStepRepository pipelineRepository;

    @KafkaListener(topics = "raw-material-produced", groupId = "processing-group")
    public void consumeRawMaterial(RawMaterialProducedEvent event) {
        log.info("Processing service received raw material event: {}", event.getBatchId());

        try {
            // Simulate async processing with delay
            CompletableFuture.runAsync(() -> {
                try {
                    // Fetch pipeline steps to determine duration
                    java.util.List<com.industry.simulator.processing.entity.PipelineStep> steps = pipelineRepository.findAllByIsActiveOrderByStepOrderAsc(true);
                    long processingDuration = steps.stream().mapToLong(s -> s.getDurationMs()).sum();
                    
                    if (processingDuration <= 0) {
                        processingDuration = 2000; // Fallback to 2 seconds if no steps configured
                    }

                    log.info("Starting processing for batch {} using {} pipeline steps. Total duration: {}ms", 
                             event.getBatchId(), steps.size(), processingDuration);
                    
                    Thread.sleep(processingDuration);

                    // Save processed material to database
                    ProcessedMaterial processed = ProcessedMaterial.builder()
                            .batchId(event.getBatchId())
                            .materialName(event.getMaterial().getName())
                            .materialType(event.getMaterial().getType())
                            .quantity(event.getQuantity())
                            .unit(event.getUnit())
                            .processingType(determineProcessingType(event.getMaterial().getType()))
                            .processingDurationMs(processingDuration)
                            .purpose(event.getPurpose())
                            .success(true)
                            .errorMessage(null)
                            .createdAt(LocalDateTime.now())
                            .completedAt(LocalDateTime.now())
                            .build();

                    repository.save(processed);
                    log.info("Processing completed for batch {}", event.getBatchId());

                    // Publish processing completed event
                    ProcessingCompletedEvent completedEvent = ProcessingCompletedEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .batchId(event.getBatchId())
                            .processedMaterial(event.getMaterial())
                            .processingType(processed.getProcessingType())
                            .processingDurationMs(processingDuration)
                            .timestamp(LocalDateTime.now())
                            .purpose(event.getPurpose())
                            .success(true)
                            .errorMessage(null)
                            .build();

                    producer.publishProcessingCompleted(completedEvent);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Processing interrupted for batch {}", event.getBatchId(), e);
                    handleProcessingError(event);
                }
            });

        } catch (Exception e) {
            log.error("Error processing raw material event", e);
            handleProcessingError(event);
        }
    }

    private String determineProcessingType(String materialType) {
        String lower = materialType.toLowerCase();
        if ("steel".equals(lower) || "aluminum".equals(lower)) {
            return "melting";
        } else if ("plastic".equals(lower) || "rubber".equals(lower)) {
            return "molding";
        } else if ("wood".equals(lower)) {
            return "cutting";
        } else {
            return "processing";
        }
    }

    private void handleProcessingError(RawMaterialProducedEvent event) {
        try {
            ProcessedMaterial errorMaterial = ProcessedMaterial.builder()
                    .batchId(event.getBatchId())
                    .materialName(event.getMaterial().getName())
                    .materialType(event.getMaterial().getType())
                    .quantity(event.getQuantity())
                    .unit(event.getUnit())
                    .processingType("unknown")
                    .processingDurationMs(0)
                    .purpose(event.getPurpose())
                    .success(false)
                    .errorMessage("Processing failed")
                    .createdAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();

            repository.save(errorMaterial);
        } catch (Exception e) {
            log.error("Error saving failed processing record", e);
        }
    }
}
