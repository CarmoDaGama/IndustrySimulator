package com.industry.simulator.assembly.controller;

import com.industry.simulator.assembly.websocket.OrderStatusPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/ws")
@RequiredArgsConstructor
public class OrderStatusController {

    private final OrderStatusPublisher orderStatusPublisher;

    @PostMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam String status,
            @RequestParam(required = false) String message) {
        
        switch (status.toUpperCase()) {
            case "PROCESSING":
                orderStatusPublisher.publishOrderProcessing(orderId);
                break;
            case "ASSEMBLED":
                orderStatusPublisher.publishOrderAssembled(orderId, null, null, null);
                break;
            case "SHIPPED":
                orderStatusPublisher.publishOrderShipped(orderId);
                break;
            case "COMPLETED":
                orderStatusPublisher.publishOrderCompleted(orderId);
                break;
            case "FAILED":
                orderStatusPublisher.publishOrderFailed(orderId, message != null ? message : "Unknown error");
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }
}
