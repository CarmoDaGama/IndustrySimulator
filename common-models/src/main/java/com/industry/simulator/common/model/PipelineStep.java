package com.industry.simulator.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents a step in a processing pipeline with simulated duration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStep implements Serializable {
    private static final long serialVersionUID = 1L;

    private String stepName;
    private long durationMs; // Duration in milliseconds (simulates real processing time)
    private String description;
    private boolean critical; // If true, failure here stops the entire pipeline
}
