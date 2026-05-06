package com.industry.simulator.assembly.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productId;
    private String productName;
    private String batchId;
    private int componentCount;
    private boolean assembled;
    private String status; // "PENDING", "ASSEMBLING", "COMPLETED", "FAILED"
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime assembledAt;
}
