package com.industry.simulator.rawmaterial.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawMaterialResponse {
    private String id;
    private String materialName;
    private String materialType;
    private double quantity;
    private String unit;
    private String batchId;
    private LocalDateTime createdAt;
}
