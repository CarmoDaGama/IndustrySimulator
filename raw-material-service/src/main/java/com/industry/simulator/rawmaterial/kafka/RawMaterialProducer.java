package com.industry.simulator.rawmaterial.kafka;

import com.industry.simulator.common.events.RawMaterialProducedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RawMaterialProducer {

    @Autowired
    private KafkaTemplate<String, RawMaterialProducedEvent> kafkaTemplate;

    public void publishRawMaterialProduced(RawMaterialProducedEvent event) {
        log.info("Publishing raw material produced event: {}", event.getBatchId());
        kafkaTemplate.send("raw-material-produced", event.getBatchId(), event);
    }
}
