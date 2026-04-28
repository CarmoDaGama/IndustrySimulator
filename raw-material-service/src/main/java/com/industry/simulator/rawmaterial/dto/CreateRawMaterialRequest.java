package com.industry.simulator.rawmaterial.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRawMaterialRequest {
    private String materialName;
    private String materialType;
    private double quantity;
    private String unit;
}
