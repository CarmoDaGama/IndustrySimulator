package com.industry.simulator.assembly.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderStatusPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishOrderStatus(String orderId, String status, String productType, 
                                   Integer quantity, String customerName, String message) {
        OrderStatusEvent event = new OrderStatusEvent();
        event.setOrderId(orderId);
        event.setStatus(status);
        event.setProductType(productType);
        event.setQuantity(quantity);
        event.setCustomerName(customerName);
        event.setUpdatedAt(LocalDateTime.now());
        event.setMessage(message);

        // Publish to specific order subscription
        messagingTemplate.convertAndSend(
                "/topic/orders/" + orderId,
                event
        );

        // Also publish to general orders feed
        messagingTemplate.convertAndSend(
                "/topic/orders/all",
                event
        );
    }

    public void publishOrderCreated(String orderId, String productType, Integer quantity, String customerName) {
        publishOrderStatus(orderId, OrderStatusEvent.Status.PENDING.toString(), productType, 
                         quantity, customerName, "Order created and pending processing");
    }

    public void publishOrderProcessing(String orderId) {
        publishOrderStatus(orderId, OrderStatusEvent.Status.PROCESSING.toString(), null, 
                         null, null, "Order is being processed");
    }

    public void publishOrderAssembled(String orderId, String productType, Integer quantity, String customerName) {
        publishOrderStatus(orderId, OrderStatusEvent.Status.ASSEMBLED.toString(), productType, 
                         quantity, customerName, "Product assembled successfully");
    }

    public void publishOrderShipped(String orderId) {
        publishOrderStatus(orderId, OrderStatusEvent.Status.SHIPPED.toString(), null, 
                         null, null, "Order shipped to customer");
    }

    public void publishOrderCompleted(String orderId) {
        publishOrderStatus(orderId, OrderStatusEvent.Status.COMPLETED.toString(), null, 
                         null, null, "Order completed");
    }

    public void publishOrderFailed(String orderId, String errorMessage) {
        publishOrderStatus(orderId, OrderStatusEvent.Status.FAILED.toString(), null, 
                         null, null, "Order failed: " + errorMessage);
    }
}
