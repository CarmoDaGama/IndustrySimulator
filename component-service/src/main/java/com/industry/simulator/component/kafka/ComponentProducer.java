package com.industry.simulator.component.kafka;

import com.industry.simulator.common.events.ComponentAssembledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Kafka producer for component assembled events with logging
 */
@Service
public class ComponentProducer {

    private static final Logger log = LoggerFactory.getLogger(ComponentProducer.class);

    @Autowired
    private KafkaTemplate<String, ComponentAssembledEvent> kafkaTemplate;

    private static final String TOPIC = "component-assembled";

    public void publishComponentAssembled(ComponentAssembledEvent event) {
        String correlationId = UUID.randomUUID().toString();
        
        try {
            log.info("Publishing component event [batch={}, correlationId={}]", 
                    event.getBatchId(), correlationId);

            kafkaTemplate.send(TOPIC, event.getBatchId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            log.info("Successfully published component event [batch={}, correlationId={}, partition={}, offset={}]",
                                    event.getBatchId(),
                                    correlationId,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish component event [batch={}, correlationId={}]", 
                                    event.getBatchId(), correlationId, exception);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing component [batch={}, correlationId={}]", 
                    event.getBatchId(), correlationId, e);
        }
    }
}
