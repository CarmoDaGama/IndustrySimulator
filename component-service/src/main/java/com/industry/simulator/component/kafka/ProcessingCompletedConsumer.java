package com.industry.simulator.component.kafka;

import com.industry.simulator.common.events.ProcessingCompletedEvent;
import com.industry.simulator.component.service.ComponentWorkerPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Apenas enfileira o evento para a pool de Workers da camada de
 * Componentes. Ver {@link ComponentWorkerPoolService}.
 */
@Service
public class ProcessingCompletedConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProcessingCompletedConsumer.class);

    @Autowired
    private ComponentWorkerPoolService workerPoolService;

    @KafkaListener(topics = "processing-completed", groupId = "component-group")
    public void consumeProcessingCompleted(ProcessingCompletedEvent event) {
        log.info("{} | component-service | Evento recebido, entregue à pool de workers (fila: {})",
                event.getBatchId(), workerPoolService.getQueueSize());
        workerPoolService.submit(event);
    }
}
