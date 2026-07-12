package com.industry.simulator.assembly.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Regra de producao configuravel (Seccao 6.3 do enunciado): que insumos sao
 * consumidos e o que produzem. Como suporta varios inputs, a mesma tabela
 * representa tanto as regras de transformacao (Ferro x2 -> Aco x1) como a
 * arvore de componentes / BOM (Motor x1 + Pneus x4 -> Carro x1).
 */
@Entity
@Table(name = "production_rule")
public class ProductionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String outputMaterial;

    private String outputType;

    @Column(nullable = false)
    private int outputQuantity = 1;

    private String factory;

    // purpose obrigatorio do contrato de dados
    private String targetProduct;
    private String targetComponent;
    @Column(length = 500)
    private String description;

    private boolean active = true;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ProductionRuleInput> inputs = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOutputMaterial() { return outputMaterial; }
    public void setOutputMaterial(String outputMaterial) { this.outputMaterial = outputMaterial; }
    public String getOutputType() { return outputType; }
    public void setOutputType(String outputType) { this.outputType = outputType; }
    public int getOutputQuantity() { return outputQuantity; }
    public void setOutputQuantity(int outputQuantity) { this.outputQuantity = outputQuantity; }
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
    public List<ProductionRuleInput> getInputs() { return inputs; }
    public void setInputs(List<ProductionRuleInput> inputs) { this.inputs = inputs; }
}
