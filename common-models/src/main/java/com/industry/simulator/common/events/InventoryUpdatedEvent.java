package com.industry.simulator.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published when inventory is updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryUpdatedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String componentId;
    private String componentName;
    private double quantityBefore;
    private double quantityAfter;
    private String operation; // "ADD", "REMOVE", "CONSUME"
    private LocalDateTime timestamp;
    private String purpose; // v2 requirement
    private String reason; // e.g., "production", "order_fulfillment"
}
