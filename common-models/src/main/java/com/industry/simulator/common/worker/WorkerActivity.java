package com.industry.simulator.common.worker;

/**
 * Fotografia do que um Worker (Thread) está a fazer neste instante.
 * É o que permite ao portal mostrar cada etapa de cada camada em execução,
 * em vez de apenas o resultado final.
 */
public class WorkerActivity {

    public enum State {
        /** À espera de matéria-prima/insumos da camada anterior (bloqueio por escassez). */
        BLOCKED,
        /** A executar uma etapa da pipeline. */
        RUNNING,
        /** Sem trabalho e sem bloqueio (ex.: sem configuração activa). */
        IDLE
    }

    private String workerName;
    private State state;
    private String currentStep;
    private int stepIndex;
    private int totalSteps;
    private long stepDurationMs;
    private long stepElapsedMs;
    private String batchIds;

    public WorkerActivity() {
    }

    public WorkerActivity(String workerName, State state, String currentStep, int stepIndex,
                          int totalSteps, long stepDurationMs, long stepElapsedMs, String batchIds) {
        this.workerName = workerName;
        this.state = state;
        this.currentStep = currentStep;
        this.stepIndex = stepIndex;
        this.totalSteps = totalSteps;
        this.stepDurationMs = stepDurationMs;
        this.stepElapsedMs = stepElapsedMs;
        this.batchIds = batchIds;
    }

    public String getWorkerName() { return workerName; }
    public void setWorkerName(String workerName) { this.workerName = workerName; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public int getStepIndex() { return stepIndex; }
    public void setStepIndex(int stepIndex) { this.stepIndex = stepIndex; }
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    public long getStepDurationMs() { return stepDurationMs; }
    public void setStepDurationMs(long stepDurationMs) { this.stepDurationMs = stepDurationMs; }
    public long getStepElapsedMs() { return stepElapsedMs; }
    public void setStepElapsedMs(long stepElapsedMs) { this.stepElapsedMs = stepElapsedMs; }
    public String getBatchIds() { return batchIds; }
    public void setBatchIds(String batchIds) { this.batchIds = batchIds; }
}
