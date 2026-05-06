package com.industry.simulator.assembly.kafka;

import com.industry.simulator.common.events.ProductAssembledEvent;
import com.industry.simulator.common.events.InventoryUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AssemblyProducer {

    @Autowired
    private KafkaTemplate<String, ProductAssembledEvent> productTemplate;

    @Autowired
    private KafkaTemplate<String, InventoryUpdatedEvent> inventoryTemplate;

    public void publishProductAssembled(ProductAssembledEvent event) {
        log.info("Publishing product assembled event: {}", event.getProductId());
        productTemplate.send("product-assembled", event.getProductId(), event);
    }

    public void publishInventoryUpdated(InventoryUpdatedEvent event) {
        log.info("Publishing inventory updated event: {}", event.getComponentId());
        inventoryTemplate.send("inventory-updated", event.getComponentId(), event);
    }
}
