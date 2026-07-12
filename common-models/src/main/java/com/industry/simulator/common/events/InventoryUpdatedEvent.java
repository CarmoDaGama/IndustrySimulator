package com.industry.simulator.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Notificação da Camada 5. Mantém o mesmo envelope, mas o payload descreve a
 * alteração de stock (não é um nó da árvore de produção).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryUpdatedEvent implements Serializable {
    private static final long serialVersionUID = 2L;

    private String eventId;
    @Builder.Default
    private String eventType = "INVENTORY_UPDATED";
    private long timestamp;
    private Payload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Payload implements Serializable {
        private String id;
        private String name;
        private double quantityBefore;
        private double quantityAfter;
        private String operation; // ADD, REMOVE, RESERVE
        private String reason;
    }
}
