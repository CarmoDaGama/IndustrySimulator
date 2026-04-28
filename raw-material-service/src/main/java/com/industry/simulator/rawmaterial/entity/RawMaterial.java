package com.industry.simulator.rawmaterial.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "raw_materials")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String materialName;
    private String materialType;
    private double quantity;
    private String unit;
    private String batchId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
