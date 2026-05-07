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
public class MarketOrderRequest {
    private String productType;
    private int quantity;
    private String bomVersion;
    private String customerName;
    private LocalDateTime requiredDeliveryDate;
    private int priority;
}
