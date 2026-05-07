package com.industry.simulator.assembly.kafka;

import com.industry.simulator.common.events.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventBridge {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "raw-material-produced", groupId = "event-monitor-group")
    public void bridgeRawMaterial(RawMaterialProducedEvent event) {
        broadcast("RawMaterialProduced", event.getEventId(), event.getBatchId(), "COMPLETED", "production", event);
    }

    @KafkaListener(topics = "processing-completed", groupId = "event-monitor-group")
    public void bridgeProcessing(ProcessingCompletedEvent event) {
        broadcast("ProcessingCompleted", event.getEventId(), event.getBatchId(), event.isSuccess() ? "COMPLETED" : "FAILED", "production", event);
    }

    @KafkaListener(topics = "component-assembled", groupId = "event-monitor-group")
    public void bridgeComponentAssembled(ComponentAssembledEvent event) {
        broadcast("ComponentAssembled", event.getEventId(), event.getBatchId(), "COMPLETED", "production", event);
    }

    @KafkaListener(topics = "product-assembled", groupId = "event-monitor-group")
    public void bridgeProductAssembled(ProductAssembledEvent event) {
        broadcast("ProductAssembled", event.getEventId(), event.getBatchId(), event.isSuccess() ? "COMPLETED" : "FAILED", "production", event);
    }

    @KafkaListener(topics = "inventory-updated", groupId = "event-monitor-group")
    public void bridgeInventory(InventoryUpdatedEvent event) {
        broadcast("InventoryUpdated", event.getEventId(), event.getComponentId(), "COMPLETED", "inventory", event);
    }

    private void broadcast(String type, String eventId, String batchId, String status, String purpose, Object details) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", type);
        payload.put("batchId", batchId);
        payload.put("status", status);
        payload.put("purpose", purpose);
        payload.put("timestamp", java.time.LocalDateTime.now().toString());
        payload.put("details", details);

        log.debug("Bridging Kafka event to WebSocket: {}", type);
        messagingTemplate.convertAndSend("/topic/events", payload);
    }
}
