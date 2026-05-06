package com.industry.simulator.assembly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private Long id;
    private String productId;
    private String productName;
    private int quantity;
    private int reservedQuantity;
    private int availableQuantity;
    private String location;
    private LocalDateTime lastUpdated;
}
