package com.industry.simulator.component.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentRequest {
    
    @NotBlank(message = "Component name cannot be blank")
    @Size(min = 2, max = 100, message = "Component name must be between 2 and 100 characters")
    private String componentName;
    
    @NotBlank(message = "Component type cannot be blank")
    @Size(min = 2, max = 50, message = "Component type must be between 2 and 50 characters")
    private String componentType;
    
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @DecimalMax(value = "100000.00", message = "Quantity cannot exceed 100000")
    private double quantity;
    
    @NotBlank(message = "Unit cannot be blank")
    @Size(min = 1, max = 10, message = "Unit must be between 1 and 10 characters")
    private String unit;
}
