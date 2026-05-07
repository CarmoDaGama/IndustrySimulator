package com.industry.simulator.rawmaterial.kafka;

import com.industry.simulator.common.events.RawMaterialProducedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Kafka producer for raw material events with logging
 */
@Component
@Slf4j
public class RawMaterialProducer {

    @Autowired
    private KafkaTemplate<String, RawMaterialProducedEvent> kafkaTemplate;

    private static final String TOPIC = "raw-material-produced";

    public void publishRawMaterialProduced(RawMaterialProducedEvent event) {
        String correlationId = UUID.randomUUID().toString();
        
        try {
            log.info("Publishing raw material event [batch={}, correlationId={}]", 
                    event.getBatchId(), correlationId);

            kafkaTemplate.send(TOPIC, event.getBatchId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            log.info("Successfully published raw material [batch={}, correlationId={}, partition={}, offset={}]",
                                    event.getBatchId(),
                                    correlationId,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish raw material event [batch={}, correlationId={}]", 
                                    event.getBatchId(), correlationId, exception);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing raw material [batch={}, correlationId={}]", 
                    event.getBatchId(), correlationId, e);
        }
    }
}
