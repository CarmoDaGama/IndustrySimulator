package com.industry.simulator.rawmaterial.dto;

import jakarta.validation.constraints.*;

public class CreateRawMaterialRequest {
    
    @NotBlank(message = "Material name cannot be blank")
    @Size(min = 2, max = 100, message = "Material name must be between 2 and 100 characters")
    private String materialName;
    
    @NotBlank(message = "Material type cannot be blank")
    @Size(min = 2, max = 50, message = "Material type must be between 2 and 50 characters")
    private String materialType;
    
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @DecimalMax(value = "100000.00", message = "Quantity cannot exceed 100000")
    private double quantity;
    
    @NotBlank(message = "Unit cannot be blank")
    @Size(min = 1, max = 10, message = "Unit must be between 1 and 10 characters")
    private String unit;

    public CreateRawMaterialRequest() {}

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
