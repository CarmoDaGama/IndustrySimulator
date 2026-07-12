package com.industry.simulator.processing.kafka;

import com.industry.simulator.common.events.RawMaterialProducedEvent;
import com.industry.simulator.processing.service.ProcessingWorkerPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Apenas enfileira o evento recebido para a pool de Workers da camada de
 * Processamento. O trabalho pesado (bloqueio por escassez, simulação de
 * tempo de pipeline e regra 2:1) é feito pelos Workers em
 * {@link ProcessingWorkerPoolService}.
 */
@Component
public class RawMaterialConsumer {

    private static final Logger log = LoggerFactory.getLogger(RawMaterialConsumer.class);

    @Autowired
    private ProcessingWorkerPoolService workerPoolService;

    @KafkaListener(topics = "raw-material-produced", groupId = "processing-group")
    public void consumeRawMaterial(RawMaterialProducedEvent event) {
        log.info("{} | processing-service | Evento recebido, entregue à pool de workers (fila: {})",
                event.getPayload().getBatchId(), workerPoolService.getQueueSize());
        workerPoolService.submit(event);
    }
}
