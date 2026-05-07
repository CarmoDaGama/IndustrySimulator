package com.industry.simulator.assembly.dto;

import jakarta.validation.constraints.*;
import com.industry.simulator.assembly.validation.ValidBOMVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketOrderRequest {
    
    @NotBlank(message = "Product type cannot be blank")
    @Size(min = 2, max = 50, message = "Product type must be between 2 and 50 characters")
    private String productType;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 1000, message = "Quantity cannot exceed 1000")
    private int quantity;
    
    @NotBlank(message = "BOM version cannot be blank")
    @ValidBOMVersion(message = "BOM version must follow format: vX.Y.Z (e.g., v1.0.0)")
    private String bomVersion;
    
    @NotBlank(message = "Customer name cannot be blank")
    @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
    private String customerName;
    
    @NotNull(message = "Required delivery date cannot be null")
    @Future(message = "Required delivery date must be in the future")
    private LocalDateTime requiredDeliveryDate;
    
    @Min(value = 1, message = "Priority must be between 1 and 4")
    @Max(value = 4, message = "Priority must be between 1 and 4")
    private int priority;
}
