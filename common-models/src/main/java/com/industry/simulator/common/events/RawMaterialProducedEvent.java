package com.industry.simulator.common.events;

import com.industry.simulator.common.model.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published by raw-material-service when raw materials are produced
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawMaterialProducedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String batchId;
    private Component material;
    private double quantity;
    private String unit;
    private LocalDateTime timestamp;
    private String purpose; // v2 requirement: purpose of material (e.g., "assembly", "testing")
    private String sourceService;
}
