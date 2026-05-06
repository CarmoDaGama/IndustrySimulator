package com.industry.simulator.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingResponse {
    private Long id;
    private String batchId;
    private String materialName;
    private String materialType;
    private double quantity;
    private String unit;
    private String processingType;
    private long processingDurationMs;
    private String purpose;
    private boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
