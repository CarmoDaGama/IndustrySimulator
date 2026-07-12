package com.industry.simulator.common.worker;

/**
 * Uma etapa da pipeline tal como o Worker a executa: nome e duração.
 * Vem sempre de configuração (BD), nunca fixa no código.
 */
public class StepSpec {

    private final String name;
    private final long durationMs;

    public StepSpec(String name, long durationMs) {
        this.name = name;
        this.durationMs = durationMs;
    }

    public String getName() {
        return name;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
