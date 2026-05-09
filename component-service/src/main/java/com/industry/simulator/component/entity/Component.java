package com.industry.simulator.component.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "components")
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getProcessingType() { return processingType; }
    public void setProcessingType(String processingType) { this.processingType = processingType; }
    public boolean isBomValidated() { return bomValidated; }
    public void setBomValidated(boolean bomValidated) { this.bomValidated = bomValidated; }
    public boolean isCompatible() { return compatible; }
    public void setCompatible(boolean compatible) { this.compatible = compatible; }
    public String getCompatibilityNotes() { return compatibilityNotes; }
    public void setCompatibilityNotes(String compatibilityNotes) { this.compatibilityNotes = compatibilityNotes; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public boolean isAssembled() { return assembled; }
    public void setAssembled(boolean assembled) { this.assembled = assembled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getAssembledAt() { return assembledAt; }
    public void setAssembledAt(LocalDateTime assembledAt) { this.assembledAt = assembledAt; }
}
