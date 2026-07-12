package com.industry.simulator.common.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Pool de Workers genérico e reutilizável, partilhado por todos os
 * microserviços da cadeia de valor. Cada Worker é uma Thread real que
 * representa uma linha de produção activa:
 *
 * <ol>
 *   <li>Consome N unidades da camada anterior de uma fila interna
 *       (por omissão N=2, cumprindo a "Regra de Consumo da Cadeia 2:1").</li>
 *   <li>Bloqueia automaticamente em {@link BlockingQueue#take()} enquanto os
 *       recursos necessários não estiverem disponíveis ("Bloqueio por
 *       Escassez").</li>
 *   <li>Executa a pipeline <b>etapa a etapa</b>, respeitando a duração de cada
 *       uma (lida em tempo real da configuração em BD) e publicando o seu
 *       estado, para que o portal possa mostrar a etapa em execução.</li>
 *   <li>Produz 1 unidade de saída e entrega-a ao consumidor fornecido
 *       (normalmente publica um evento Kafka).</li>
 * </ol>
 *
 * O número de Workers activos é parametrizável em runtime através de
 * {@link #resize(int)}, permitindo que o portal de configurações ajuste o
 * paralelismo de cada microserviço sem reiniciar o processo.
 *
 * @param <IN>  tipo dos itens de entrada (unidades da camada anterior)
 * @param <OUT> tipo do item produzido (unidade desta camada)
 */
public class TwoToOneWorkerPool<IN, OUT> {

    private static final Logger log = LoggerFactory.getLogger(TwoToOneWorkerPool.class);

    private final String namePrefix;
    private final int inputsPerOutput;
    private final Supplier<List<StepSpec>> stepsSupplier;
    private final Function<List<IN>, OUT> producer;
    private final Consumer<OUT> onProduced;
    private final BiConsumer<Exception, List<IN>> onError;
    private final Function<IN, String> batchIdOf;

    private final BlockingQueue<IN> inputQueue = new LinkedBlockingQueue<>();
    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private int nextWorkerId = 0;

    public TwoToOneWorkerPool(String namePrefix,
                               int inputsPerOutput,
                               Supplier<List<StepSpec>> stepsSupplier,
                               Function<List<IN>, OUT> producer,
                               Consumer<OUT> onProduced,
                               BiConsumer<Exception, List<IN>> onError,
                               Function<IN, String> batchIdOf) {
        this.namePrefix = namePrefix;
        this.inputsPerOutput = Math.max(1, inputsPerOutput);
        this.stepsSupplier = stepsSupplier;
        this.producer = producer;
        this.onProduced = onProduced;
        this.onError = onError;
        this.batchIdOf = batchIdOf;
    }

    /** Entrega uma unidade da camada anterior à fila interna do pool. */
    public void submit(IN item) {
        inputQueue.add(item);
    }

    public int getQueueSize() {
        return inputQueue.size();
    }

    public int getWorkerCount() {
        return workers.size();
    }

    /** Estado actual de cada Worker — alimenta a monitorização do portal. */
    public List<WorkerActivity> getActivities() {
        return workers.stream().map(Worker::snapshot).collect(Collectors.toList());
    }

    /** Ajusta o número de Workers (Threads) activos para o valor pretendido. */
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

        // Estado observável pelo portal (escrito só por esta Thread, lido por outras).
        private volatile WorkerActivity.State state = WorkerActivity.State.BLOCKED;
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

        private void markBlocked() {
            state = WorkerActivity.State.BLOCKED;
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
                List<IN> batch = new ArrayList<>(inputsPerOutput);
                try {
                    // Bloqueio por escassez: espera até existirem 'inputsPerOutput'
                    // unidades disponíveis da camada anterior.
                    markBlocked();
                    for (int i = 0; i < inputsPerOutput; i++) {
                        IN item = inputQueue.take();
                        batch.add(item);
                    }

                    batchIds = batch.stream().map(batchIdOf).collect(Collectors.joining(","));

                    // Executa a pipeline etapa a etapa, para que cada uma seja
                    // observável enquanto decorre.
                    List<StepSpec> steps = stepsSupplier.get();
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

                    OUT out = producer.apply(batch);
                    onProduced.accept(out);
                    markBlocked();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (!active) {
                        log.info("[{}] terminado", getName());
                        return;
                    }
                } catch (Exception ex) {
                    log.error("[{}] falha ao processar lote: {}", getName(), ex.getMessage(), ex);
                    if (onError != null) {
                        onError.accept(ex, batch);
                    }
                    markBlocked();
                }
            }
        }
    }
}
