package com.industry.simulator.common.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Pool de Workers genérico para camadas sem dependência da camada anterior
 * (Camada 1 - Extracção). Cada Worker é uma Thread que corre continuamente e
 * autonomamente: produz uma unidade sempre que existir configuração
 * disponível, ou aguarda caso ainda não exista nenhuma configuração activa.
 *
 * Tal como o {@link TwoToOneWorkerPool}, executa a produção etapa a etapa e
 * publica o estado de cada Worker, para que o portal possa mostrar a etapa em
 * execução (ex.: EXTRACTION, TRANSPORT).
 *
 * @param <OUT> tipo do item produzido em cada ciclo
 */
public class ContinuousWorkerPool<OUT> {

    private static final Logger log = LoggerFactory.getLogger(ContinuousWorkerPool.class);
    private static final long IDLE_RETRY_MS = 1000L;

    /**
     * Um ciclo de produção: as etapas a executar e o item a produzir no fim.
     * Preparado pelo serviço a cada ciclo, já com os valores vindos da BD.
     */
    public static class Cycle<OUT> {
        private final List<StepSpec> steps;
        private final String batchId;
        private final Supplier<OUT> result;

        public Cycle(List<StepSpec> steps, String batchId, Supplier<OUT> result) {
            this.steps = steps;
            this.batchId = batchId;
            this.result = result;
        }

        public List<StepSpec> getSteps() { return steps; }
        public String getBatchId() { return batchId; }
        public OUT produce() { return result.get(); }
    }

    /** Prepara o próximo ciclo, ou devolve null se não houver configuração activa. */
    @FunctionalInterface
    public interface CycleFactory<OUT> {
        Cycle<OUT> next();
    }

    private final String namePrefix;
    private final CycleFactory<OUT> cycleFactory;
    private final Consumer<OUT> onProduced;
    private final Consumer<Exception> onError;

    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private int nextWorkerId = 0;

    public ContinuousWorkerPool(String namePrefix,
                                 CycleFactory<OUT> cycleFactory,
                                 Consumer<OUT> onProduced,
                                 Consumer<Exception> onError) {
        this.namePrefix = namePrefix;
        this.cycleFactory = cycleFactory;
        this.onProduced = onProduced;
        this.onError = onError;
    }

    public int getWorkerCount() {
        return workers.size();
    }

    /** Estado actual de cada Worker — alimenta a monitorização do portal. */
    public List<WorkerActivity> getActivities() {
        return workers.stream().map(Worker::snapshot).collect(Collectors.toList());
    }

    public synchronized void resize(int desiredWorkerCount) {
        int target = Math.max(0, desiredWorkerCount);
        while (workers.size() < target) {
            Worker worker = new Worker(namePrefix + "-worker-" + (nextWorkerId++));
            workers.add(worker);
            worker.start();
        }
        while (workers.size() > target) {
            Worker worker = workers.remove(workers.size() - 1);
            worker.shutdown();
        }
        log.info("[{}] Pool redimensionado para {} worker(s)", namePrefix, workers.size());
    }

    public synchronized void shutdownAll() {
        for (Worker worker : workers) {
            worker.shutdown();
        }
        workers.clear();
    }

    private class Worker extends Thread {
        private volatile boolean active = true;

        private volatile WorkerActivity.State state = WorkerActivity.State.IDLE;
        private volatile String currentStep = null;
        private volatile int stepIndex = 0;
        private volatile int totalSteps = 0;
        private volatile long stepDurationMs = 0;
        private volatile long stepStartedAt = 0;
        private volatile String batchIds = null;

        Worker(String name) {
            super(name);
            setDaemon(true);
        }

        void shutdown() {
            active = false;
            this.interrupt();
        }

        WorkerActivity snapshot() {
            long elapsed = state == WorkerActivity.State.RUNNING && stepStartedAt > 0
                    ? System.currentTimeMillis() - stepStartedAt
                    : 0;
            return new WorkerActivity(getName(), state, currentStep, stepIndex,
                    totalSteps, stepDurationMs, Math.min(elapsed, stepDurationMs), batchIds);
        }

        private void markIdle() {
            state = WorkerActivity.State.IDLE;
            currentStep = null;
            stepIndex = 0;
            totalSteps = 0;
            stepDurationMs = 0;
            stepStartedAt = 0;
            batchIds = null;
        }

        @Override
        public void run() {
            log.info("[{}] iniciado", getName());
            while (active) {
                try {
                    Cycle<OUT> cycle = cycleFactory.next();
                    if (cycle == null) {
                        // Sem configuração activa: aguarda e tenta de novo.
                        markIdle();
                        Thread.sleep(IDLE_RETRY_MS);
                        continue;
                    }

                    batchIds = cycle.getBatchId();
                    List<StepSpec> steps = cycle.getSteps();
                    totalSteps = steps.size();

                    for (int i = 0; i < steps.size(); i++) {
                        StepSpec step = steps.get(i);
                        state = WorkerActivity.State.RUNNING;
                        currentStep = step.getName();
                        stepIndex = i + 1;
                        stepDurationMs = step.getDurationMs();
                        stepStartedAt = System.currentTimeMillis();

                        log.info("[{}] etapa {}/{} '{}' ({}ms) lote={}",
                                getName(), stepIndex, totalSteps, currentStep, stepDurationMs, batchIds);

                        if (step.getDurationMs() > 0) {
                            Thread.sleep(step.getDurationMs());
                        }
                    }

                    OUT out = cycle.produce();
                    if (out != null) {
                        onProduced.accept(out);
                    }
                    markIdle();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (!active) {
                        log.info("[{}] terminado", getName());
                        return;
                    }
                } catch (Exception ex) {
                    log.error("[{}] falha no ciclo de produção: {}", getName(), ex.getMessage(), ex);
                    if (onError != null) {
                        onError.accept(ex);
                    }
                    markIdle();
                }
            }
        }
    }
}
