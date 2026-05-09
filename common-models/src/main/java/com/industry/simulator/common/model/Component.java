package com.industry.simulator.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a component in the industrial supply chain
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private boolean compatibleForAssembly;

    // v2 mandatory fields
    private Producer producer;
    
    @Builder.Default
    private List<Component> components = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Producer implements Serializable {
        private String service;
        private String factory;
    }
}
