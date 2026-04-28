package com.industry.simulator.common.events;

import com.industry.simulator.common.model.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published by component-service when components are assembled
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentAssembledEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String batchId;
    private Component finalComponent;
    private List<String> usedMaterialIds;
    private long assemblyDurationMs;
    private LocalDateTime timestamp;
    private String purpose; // v2 requirement
    private boolean success;
    private String errorMessage;
}
