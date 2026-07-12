package com.industry.simulator.assembly.entity;

import jakarta.persistence.*;

/**
 * Configuração da simulação de clientes fictícios (Camada 6 — Mercado).
 * O enunciado exige que as requisições de compra sejam geradas de forma
 * automática e assíncrona; estes parâmetros vivem em BD e são editáveis no
 * portal.
 */
@Entity
@Table(name = "customer_simulator_config")
public class CustomerSimulatorConfig {

    @Id
    private Long id = 1L;

    /** Nº de clientes fictícios em simultâneo (cada um é uma Thread). */
    @Column(nullable = false)
    private int customerCount = 0;

    /** Tempo que cada cliente "pensa" antes de encomendar. */
    @Column(nullable = false)
    private long thinkTimeMs = 8000;

    @Column(nullable = false)
    private int minQuantity = 1;

    @Column(nullable = false)
    private int maxQuantity = 3;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getCustomerCount() { return customerCount; }
    public void setCustomerCount(int customerCount) { this.customerCount = customerCount; }
    public long getThinkTimeMs() { return thinkTimeMs; }
    public void setThinkTimeMs(long thinkTimeMs) { this.thinkTimeMs = thinkTimeMs; }
    public int getMinQuantity() { return minQuantity; }
    public void setMinQuantity(int minQuantity) { this.minQuantity = minQuantity; }
    public int getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(int maxQuantity) { this.maxQuantity = maxQuantity; }
}
