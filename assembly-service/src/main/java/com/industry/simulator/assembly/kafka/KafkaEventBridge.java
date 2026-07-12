package com.industry.simulator.assembly.kafka;

import com.industry.simulator.common.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Espelha os eventos Kafka para o portal (WebSocket), preservando o envelope
 * padrão {eventId, eventType, timestamp, payload}. É só leitura/monitorização:
 * não interfere no fluxo de produção.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventBridge {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "raw-material-produced", groupId = "event-monitor-group")
    public void bridgeRawMaterial(RawMaterialProducedEvent event) {
        broadcast(event.getEventId(), event.getEventType(), event.getTimestamp(), event.getPayload());
    }

    @KafkaListener(topics = "processing-completed", groupId = "event-monitor-group")
    public void bridgeProcessing(ProcessingCompletedEvent event) {
        broadcast(event.getEventId(), event.getEventType(), event.getTimestamp(), event.getPayload());
    }

    @KafkaListener(topics = "component-assembled", groupId = "event-monitor-group")
    public void bridgeComponentAssembled(ComponentAssembledEvent event) {
        broadcast(event.getEventId(), event.getEventType(), event.getTimestamp(), event.getPayload());
    }

    @KafkaListener(topics = "product-assembled", groupId = "event-monitor-group")
    public void bridgeProductAssembled(ProductAssembledEvent event) {
        broadcast(event.getEventId(), event.getEventType(), event.getTimestamp(), event.getPayload());
    }

    @KafkaListener(topics = "inventory-updated", groupId = "event-monitor-group")
    public void bridgeInventory(InventoryUpdatedEvent event) {
        broadcast(event.getEventId(), event.getEventType(), event.getTimestamp(), event.getPayload());
    }

    private void broadcast(String eventId, String eventType, long timestamp, Object payload) {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("timestamp", timestamp);
        envelope.put("payload", payload);

        log.debug("Evento espelhado para o portal: {}", eventType);
        messagingTemplate.convertAndSend("/topic/events", envelope);
    }
}
