package com.industry.simulator.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a component in the industrial supply chain
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Component implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String type; // RAW_MATERIAL, PROCESSED_MATERIAL, COMPONENT, FINAL_PRODUCT
    private double quantity;
    private String unit; // kg, units, etc.
    private String batchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String sourceService; // which service created this component
    private boolean compatibleForAssembly;
}
