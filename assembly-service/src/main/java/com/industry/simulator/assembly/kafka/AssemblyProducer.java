package com.industry.simulator.assembly.kafka;

import com.industry.simulator.common.events.ProductAssembledEvent;
import com.industry.simulator.common.events.InventoryUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Kafka producer for assembly events with logging
 */
@Service
@Slf4j
public class AssemblyProducer {

    @Autowired
    private KafkaTemplate<String, ProductAssembledEvent> productTemplate;

    @Autowired
    private KafkaTemplate<String, InventoryUpdatedEvent> inventoryTemplate;

    public void publishProductAssembled(ProductAssembledEvent event) {
        String correlationId = UUID.randomUUID().toString();
        String topic = "product-assembled";
        
        try {
            log.info("Publishing product event [productId={}, correlationId={}]", 
                    event.getProductId(), correlationId);

            productTemplate.send(topic, event.getProductId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            log.info("Successfully published product event [productId={}, correlationId={}, partition={}, offset={}]",
                                    event.getProductId(),
                                    correlationId,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish product event [productId={}, correlationId={}]", 
                                    event.getProductId(), correlationId, exception);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing product [productId={}, correlationId={}]", 
                    event.getProductId(), correlationId, e);
        }
    }

    public void publishInventoryUpdated(InventoryUpdatedEvent event) {
        String correlationId = UUID.randomUUID().toString();
        String topic = "inventory-updated";
        
        try {
            log.info("Publishing inventory event [componentId={}, correlationId={}]", 
                    event.getComponentId(), correlationId);

            inventoryTemplate.send(topic, event.getComponentId(), event)
                    .whenComplete((result, exception) -> {
                        if (exception == null) {
                            log.info("Successfully published inventory event [componentId={}, correlationId={}, partition={}, offset={}]",
                                    event.getComponentId(),
                                    correlationId,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish inventory event [componentId={}, correlationId={}]", 
                                    event.getComponentId(), correlationId, exception);
                        }
                    });

        } catch (Exception e) {
            log.error("Error publishing inventory [componentId={}, correlationId={}]", 
                    event.getComponentId(), correlationId, e);
        }
    }
}
