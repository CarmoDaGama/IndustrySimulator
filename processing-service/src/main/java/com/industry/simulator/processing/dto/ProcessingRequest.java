package com.industry.simulator.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingRequest {
    private String materialName;
    private String materialType;
    private String processingType; // e.g., "melting", "cutting", "casting"
    private long processingDurationMs;
    private String purpose;
}
