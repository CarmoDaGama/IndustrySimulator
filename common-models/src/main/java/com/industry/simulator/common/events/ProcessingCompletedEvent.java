package com.industry.simulator.common.events;

import com.industry.simulator.common.model.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Evento publicado pela Camada 2 (envelope padrão do enunciado). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingCompletedEvent implements Serializable {
    private static final long serialVersionUID = 2L;

    private String eventId;
    @Builder.Default
    private String eventType = "MATERIAL_PROCESSED";
    private long timestamp;
    private Component payload;
}
