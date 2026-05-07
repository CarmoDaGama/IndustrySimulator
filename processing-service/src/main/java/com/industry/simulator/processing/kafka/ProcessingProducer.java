package com.industry.simulator.processing.kafka;

import com.industry.simulator.common.events.ProcessingCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka producer for processing completed events with logging
 */
@Component
@Slf4j
public class ProcessingProducer {

    @Autowired
    private KafkaTemplate<String, ProcessingCompletedEvent> kafkaTemplate;

    private static final String TOPIC = "processing-completed";

    public void publishProcessingCompleted(ProcessingCompletedEvent event) {
        String correlationId = UUID.randomUUID().toString();
        
        try {
            log.info("Publishing processing event [batch={}, correlationId={}]", 
                    event.getBatchId(), correlationId);

            kafkaTemplate.send(TOPIC, event.getBatchId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            log.info("Successfully published processing event [batch={}, correlationId={}, partition={}, offset={}]",
                                    event.getBatchId(),
                                    correlationId,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish processing event [batch={}, correlationId={}]", 
                                    event.getBatchId(), correlationId, exception);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing processing [batch={}, correlationId={}]", 
                    event.getBatchId(), correlationId, e);
        }
    }
}
