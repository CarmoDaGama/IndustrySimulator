package com.industry.simulator.common.events;

import com.industry.simulator.common.model.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published by processing-service when material processing is completed
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String batchId;
    private Component processedMaterial;
    private String processingType; // e.g., "melting", "cutting", "casting"
    private long processingDurationMs;
    private LocalDateTime timestamp;
    private String purpose; // v2 requirement
    private boolean success;
    private String errorMessage;
}
