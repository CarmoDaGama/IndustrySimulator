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
public class MarketOrderResponse {
    private Long id;
    private String orderId;
    private String productName;
    private int quantity;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime fulfilledAt;
}
