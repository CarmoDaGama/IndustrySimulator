package com.industry.simulator.common.events;

import com.industry.simulator.common.model.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Evento publicado pela Camada 1. Segue o envelope obrigatório do enunciado:
 * {@code {eventId, eventType, timestamp, payload}}, onde o payload é o nó da
 * árvore (id, name, purpose, producer, components).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawMaterialProducedEvent implements Serializable {
    private static final long serialVersionUID = 2L;

    private String eventId;
    @Builder.Default
    private String eventType = "RAW_MATERIAL_EXTRACTED";
    private long timestamp;
    private Component payload;
}
