package com.industry.simulator.processing.kafka;

import com.industry.simulator.common.events.RawMaterialProducedEvent;
import com.industry.simulator.common.events.ProcessingCompletedEvent;
import com.industry.simulator.processing.entity.ProcessedMaterial;
import com.industry.simulator.processing.repository.ProcessedMaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class RawMaterialConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawMaterialConsumer.class);

    @Autowired
    private ProcessedMaterialRepository repository;

    @Autowired
    private ProcessingProducer producer;

    @Autowired
    private com.industry.simulator.processing.repository.PipelineStepRepository pipelineRepository;

    @KafkaListener(topics = "raw-material-produced", groupId = "processing-group")
    public void consumeRawMaterial(RawMaterialProducedEvent event) {
        String batchId = event.getBatchId();
        log.info("{} | processing-service | Received raw material event", batchId);

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

                    log.info("{} | processing-service | Starting processing using {} steps. Duration: {}ms", 
                             batchId, steps.size(), processingDuration);
                    
                    Thread.sleep(processingDuration);

                    // Save processed material to database
                    ProcessedMaterial processed = new ProcessedMaterial();
                    processed.setBatchId(batchId);
                    processed.setMaterialName(event.getMaterial().getName());
                    processed.setMaterialType(event.getMaterial().getType());
                    processed.setQuantity(event.getQuantity());
                    processed.setUnit(event.getUnit());
                    processed.setProcessingType(determineProcessingType(event.getMaterial().getType()));
                    processed.setProcessingDurationMs(processingDuration);
                    processed.setPurpose(event.getPurpose());
                    processed.setSuccess(true);
                    processed.setCreatedAt(LocalDateTime.now());
                    processed.setCompletedAt(LocalDateTime.now());

                    repository.save(processed);
                    log.info("{} | processing-service | Processing completed", batchId);

                    // v2 Requirement: Build recursive tree
                    com.industry.simulator.common.model.Component processedComponent = com.industry.simulator.common.model.Component.builder()
                            .id("proc-" + UUID.randomUUID().toString().substring(0, 8))
                            .name("Processed " + event.getMaterial().getName())
                            .type("PROCESSED_MATERIAL")
                            .quantity(event.getQuantity())
                            .unit(event.getUnit())
                            .batchId(batchId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .producer(com.industry.simulator.common.model.Component.Producer.builder()
                                    .service("processing-service")
                                    .factory("refining-plant-beta")
                                    .build())
                            .components(Collections.singletonList(event.getMaterial()))
                            .build();

                    // Publish processing completed event
                    ProcessingCompletedEvent completedEvent = ProcessingCompletedEvent.builder()
                            .eventId(UUID.randomUUID().toString())
                            .eventType("MATERIAL_PROCESSED")
                            .batchId(batchId)
                            .processedMaterial(processedComponent)
                            .processingType(processed.getProcessingType())
                            .processingDurationMs(processingDuration)
                            .timestamp(LocalDateTime.now())
                            .purpose(event.getPurpose())
                            .success(true)
                            .build();

                    producer.publishProcessingCompleted(completedEvent);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("{} | processing-service | Processing interrupted", batchId, e);
                    handleProcessingError(event);
                }
            });

        } catch (Exception e) {
            log.error("{} | processing-service | Error processing raw material event", batchId, e);
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
        String batchId = event.getBatchId();
        try {
            ProcessedMaterial errorMaterial = new ProcessedMaterial();
            errorMaterial.setBatchId(batchId);
            errorMaterial.setMaterialName(event.getMaterial().getName());
            errorMaterial.setMaterialType(event.getMaterial().getType());
            errorMaterial.setQuantity(event.getQuantity());
            errorMaterial.setUnit(event.getUnit());
            errorMaterial.setProcessingType("unknown");
            errorMaterial.setProcessingDurationMs(0);
            errorMaterial.setPurpose(event.getPurpose());
            errorMaterial.setSuccess(false);
            errorMaterial.setErrorMessage("Processing failed");
            errorMaterial.setCreatedAt(LocalDateTime.now());
            errorMaterial.setCompletedAt(LocalDateTime.now());

            repository.save(errorMaterial);
        } catch (Exception e) {
            log.error("{} | processing-service | Error saving failed processing record", batchId, e);
        }
    }
}
