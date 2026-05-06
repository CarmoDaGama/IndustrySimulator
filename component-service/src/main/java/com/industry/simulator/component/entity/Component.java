package com.industry.simulator.component.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "components")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Component {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String batchId;
    private String componentName;
    private String componentType;
    private double quantity;
    private String unit;
    private String processingType;
    private boolean bomValidated;
    private boolean compatible;
    private String compatibilityNotes;
    private String purpose;
    private boolean assembled;
    private LocalDateTime createdAt;
    private LocalDateTime assembledAt;
}
