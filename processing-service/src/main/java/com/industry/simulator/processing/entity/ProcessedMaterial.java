package com.industry.simulator.processing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_materials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String batchId;
    private String materialName;
    private String materialType;
    private double quantity;
    private String unit;
    private String processingType; // e.g., "melting", "cutting", "casting"
    private long processingDurationMs;
    private String purpose;
    private boolean success;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
