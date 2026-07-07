package com.industry.simulator.rawmaterial.entity;

import jakarta.persistence.*;

/**
 * Configuração genérica de um recurso extraído autonomamente pela Camada 1.
 * Nada de nomes/tempos fixos no código: os Workers escolhem entre as
 * configurações activas desta tabela para decidir o que extrair e quanto
 * tempo simular (portal de configurações).
 */
@Entity
@Table(name = "extraction_config")
public class ExtractionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String materialName;
    private String materialType;
    private double quantityPerCycle = 1.0;
    private String unit = "unit";
    private long extractionDurationMs = 10000;
    private long transportDurationMs = 5000;
    private String factory = "mining-site-alpha";

    // Contrato de dados obrigatório (Secção 7): finalidade do recurso
    private String targetProduct;
    private String targetComponent;
    private String description;

    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public double getQuantityPerCycle() { return quantityPerCycle; }
    public void setQuantityPerCycle(double quantityPerCycle) { this.quantityPerCycle = quantityPerCycle; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public long getExtractionDurationMs() { return extractionDurationMs; }
    public void setExtractionDurationMs(long extractionDurationMs) { this.extractionDurationMs = extractionDurationMs; }
    public long getTransportDurationMs() { return transportDurationMs; }
    public void setTransportDurationMs(long transportDurationMs) { this.transportDurationMs = transportDurationMs; }
    public String getFactory() { return factory; }
    public void setFactory(String factory) { this.factory = factory; }
    public String getTargetProduct() { return targetProduct; }
    public void setTargetProduct(String targetProduct) { this.targetProduct = targetProduct; }
    public String getTargetComponent() { return targetComponent; }
    public void setTargetComponent(String targetComponent) { this.targetComponent = targetComponent; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
