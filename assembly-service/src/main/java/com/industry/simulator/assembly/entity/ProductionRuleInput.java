package com.industry.simulator.assembly.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/** Um insumo exigido por uma regra: quantas unidades de que material. */
@Entity
@Table(name = "production_rule_input")
public class ProductionRuleInput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String inputMaterial;

    private String inputType;

    @Column(nullable = false)
    private int inputQuantity = 2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    @JsonIgnore
    private ProductionRule rule;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInputMaterial() { return inputMaterial; }
    public void setInputMaterial(String inputMaterial) { this.inputMaterial = inputMaterial; }
    public String getInputType() { return inputType; }
    public void setInputType(String inputType) { this.inputType = inputType; }
    public int getInputQuantity() { return inputQuantity; }
    public void setInputQuantity(int inputQuantity) { this.inputQuantity = inputQuantity; }
    public ProductionRule getRule() { return rule; }
    public void setRule(ProductionRule rule) { this.rule = rule; }
}
