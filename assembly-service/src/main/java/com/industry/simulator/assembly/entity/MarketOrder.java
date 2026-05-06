package com.industry.simulator.assembly.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String productName;
    private int quantity;
    private String status; // "PENDING", "ALLOCATED", "FULFILLED", "FAILED"
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime fulfilledAt;
}
