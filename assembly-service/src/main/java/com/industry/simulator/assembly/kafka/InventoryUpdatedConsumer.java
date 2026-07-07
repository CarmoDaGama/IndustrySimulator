package com.industry.simulator.assembly.kafka;

import com.industry.simulator.assembly.service.MarketOrderService;
import com.industry.simulator.common.events.InventoryUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Camada 6 (Mercado): assim que a Camada 5 (Inventário) notifica
 * reabastecimento, os pedidos PENDENTES desse produto são reprocessados
 * automaticamente.
 */
@Service
public class InventoryUpdatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryUpdatedConsumer.class);

    @Autowired
    private MarketOrderService marketOrderService;

    @KafkaListener(topics = "inventory-updated", groupId = "assembly-market-group")
    public void consumeInventoryUpdated(InventoryUpdatedEvent event) {
        if (!"ADD".equalsIgnoreCase(event.getOperation())) {
            return;
        }
        log.info("{} | assembly-service | Stock reposto, a tentar desbloquear pedidos PENDENTES", event.getComponentName());
        marketOrderService.tryAllocatePendingOrders(event.getComponentName());
    }
}
