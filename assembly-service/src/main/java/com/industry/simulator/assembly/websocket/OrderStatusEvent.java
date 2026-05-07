package com.industry.simulator.assembly.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusEvent {
    private String orderId;
    private String status;
    private String productType;
    private Integer quantity;
    private String customerName;
    private LocalDateTime updatedAt;
    private String message;

    public enum Status {
        PENDING, PROCESSING, ASSEMBLED, SHIPPED, COMPLETED, FAILED
    }
}
