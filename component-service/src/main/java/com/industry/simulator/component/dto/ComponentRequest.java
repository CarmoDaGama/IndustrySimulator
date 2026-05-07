package com.industry.simulator.component.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentRequest {
    private String componentName;
    private String componentType;
    private double quantity;
    private String unit;
}
