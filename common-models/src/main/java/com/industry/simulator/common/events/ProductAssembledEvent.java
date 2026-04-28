package com.industry.simulator.common.events;

import com.industry.simulator.common.model.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published by assembly-service when final products are assembled
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAssembledEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String productId;
    private String batchId;
    private Component finalProduct;
    private List<String> usedComponentIds;
    private long assemblyDurationMs;
    private LocalDateTime timestamp;
    private String purpose; // v2 requirement: e.g., "market", "inventory"
    private boolean success;
    private String errorMessage;
}
