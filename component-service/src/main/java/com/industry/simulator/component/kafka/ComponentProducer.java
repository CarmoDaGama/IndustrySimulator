package com.industry.simulator.component.kafka;

import com.industry.simulator.common.events.ComponentAssembledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ComponentProducer {

    @Autowired
    private KafkaTemplate<String, ComponentAssembledEvent> kafkaTemplate;

    public void publishComponentAssembled(ComponentAssembledEvent event) {
        log.info("Publishing component assembled event: {}", event.getBatchId());
        kafkaTemplate.send("component-assembled", event.getBatchId(), event);
    }
}
