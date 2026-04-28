package com.industry.simulator.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Bill of Materials - defines required components for assembly
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BOM implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bomId;
    private String productName;
    private List<ComponentRequirement> requirements;
    private boolean isValid;
    private String validationMessage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentRequirement implements Serializable {
        private String componentName;
        private double requiredQuantity;
        private String unit;
        private boolean compatible; // Check if component is compatible with others
    }
}
