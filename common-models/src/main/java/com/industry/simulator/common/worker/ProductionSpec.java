package com.industry.simulator.common.worker;

import java.util.List;

/**
 * Uma regra de produção tal como o Worker a executa. Vem sempre de
 * configuração (BD), nunca fixa no código.
 *
 * <p>Suporta <b>vários inputs</b>, pelo que a mesma estrutura cobre os dois
 * requisitos do enunciado:
 * <ul>
 *   <li><b>Regras de transformação</b> (um input):
 *       {@code Minério de Ferro ×2 → Aço ×1}</li>
 *   <li><b>Árvore de componentes / BOM</b> (vários inputs):
 *       {@code Motor ×1 + Pneus ×4 + Chassis ×1 → Carro ×1}</li>
 * </ul>
 */
public class ProductionSpec {

    private final String outputMaterial;
    private final String outputType;
    private final int outputQuantity;
    private final String factory;
    private final String targetProduct;
    private final String targetComponent;
    private final String description;
    private final List<Input> inputs;

    public ProductionSpec(String outputMaterial, String outputType, int outputQuantity, String factory,
                          String targetProduct, String targetComponent, String description, List<Input> inputs) {
        this.outputMaterial = outputMaterial;
        this.outputType = outputType;
        this.outputQuantity = Math.max(1, outputQuantity);
        this.factory = factory;
        this.targetProduct = targetProduct;
        this.targetComponent = targetComponent;
        this.description = description;
        this.inputs = inputs;
    }

    public String getOutputMaterial() { return outputMaterial; }
    public String getOutputType() { return outputType; }
    public int getOutputQuantity() { return outputQuantity; }
    public String getFactory() { return factory; }
    public String getTargetProduct() { return targetProduct; }
    public String getTargetComponent() { return targetComponent; }
    public String getDescription() { return description; }
    public List<Input> getInputs() { return inputs; }

    /** Um insumo exigido pela regra: quantas unidades de que material. */
    public static class Input {
        private final String material;
        private final int quantity;

        public Input(String material, int quantity) {
            this.material = material;
            this.quantity = Math.max(1, quantity);
        }

        public String getMaterial() { return material; }
        public int getQuantity() { return quantity; }
    }
}
