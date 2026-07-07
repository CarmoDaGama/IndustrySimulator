package com.industry.simulator.assembly.kafka;

import com.industry.simulator.common.events.ComponentAssembledEvent;
import com.industry.simulator.assembly.service.AssemblyWorkerPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Apenas enfileira o evento para a pool de Workers da camada de
 * Montagem. Ver {@link AssemblyWorkerPoolService}.
 */
@Service
public class ComponentAssembledConsumer {

    private static final Logger log = LoggerFactory.getLogger(ComponentAssembledConsumer.class);

    @Autowired
    private AssemblyWorkerPoolService workerPoolService;

    @KafkaListener(topics = "component-assembled", groupId = "assembly-group")
    public void consumeComponentAssembled(ComponentAssembledEvent event) {
        log.info("{} | assembly-service | Evento recebido, entregue à pool de workers (fila: {})",
                event.getBatchId(), workerPoolService.getQueueSize());
        workerPoolService.submit(event);
    }
}
