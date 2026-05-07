package com.industry.simulator.assembly.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", unique = true, nullable = false)
    private String orderId;

    @Column(name = "product_type", nullable = false)
    private String productType;

    @Column(name = "bom_version")
    private String bomVersion;

    @Column(name = "requested_quantity", nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String status; // "PENDING", "ALLOCATED", "FULFILLED", "FAILED"

    private int priority;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "required_delivery_date")
    private LocalDateTime requiredDeliveryDate;

    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fulfilled_at")
    private LocalDateTime fulfilledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
