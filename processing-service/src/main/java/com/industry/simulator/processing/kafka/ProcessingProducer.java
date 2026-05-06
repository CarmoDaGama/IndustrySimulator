package com.industry.simulator.processing.kafka;

import com.industry.simulator.common.events.ProcessingCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProcessingProducer {

    @Autowired
    private KafkaTemplate<String, ProcessingCompletedEvent> kafkaTemplate;

    public void publishProcessingCompleted(ProcessingCompletedEvent event) {
        log.info("Publishing processing completed event: {}", event.getBatchId());
        kafkaTemplate.send("processing-completed", event.getBatchId(), event);
    }
}
