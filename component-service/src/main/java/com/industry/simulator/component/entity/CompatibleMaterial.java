package com.industry.simulator.component.entity;

import jakarta.persistence.*;

/**
 * Regra de compatibilidade BOM configurável (genericidade: nada de listas
 * fixas no código - a regra vive na base de dados e é editável pelo portal).
 */
@Entity
@Table(name = "compatible_material")
public class CompatibleMaterial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String materialType;

    public CompatibleMaterial() {}
    public CompatibleMaterial(String materialType) { this.materialType = materialType; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
}
