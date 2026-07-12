package com.industry.simulator.common.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Pool de Workers das camadas de transformação (2, 3 e 4). Cada Worker é uma
 * Thread real que representa uma linha de produção activa e é guiada pelas
 * <b>regras de produção configuradas em BD</b> ({@link ProductionSpec}):
 *
 * <ol>
 *   <li>Os insumos que chegam da camada anterior são arrumados em filas
 *       <b>por material</b>.</li>
 *   <li>O Worker procura uma regra cujos inputs estejam <b>todos</b>
 *       satisfeitos. Enquanto não houver nenhuma, <b>bloqueia</b> — é o
 *       "bloqueio por escassez" do enunciado. Sem regras configuradas, também
 *       fica bloqueado: "a produção inicia assim que existirem configurações e
 *       matérias-primas disponíveis".</li>
 *   <li>Consome exactamente as quantidades exigidas, executa a pipeline
 *       <b>etapa a etapa</b> e produz as unidades de saída da regra.</li>
 * </ol>
 *
 * Como a regra define quantos insumos consome, a "Regra de Consumo da Cadeia
 * (2:1)" passa a ser um mínimo garantido na configuração, e a mesma estrutura
 * suporta a árvore BOM (vários inputs distintos).
 *
 * @param <IN>  insumo vindo da camada anterior
 * @param <OUT> unidade produzida por esta camada
 */
public class ProductionWorkerPool<IN, OUT> {

    private static final Logger log = LoggerFactory.getLogger(ProductionWorkerPool.class);

    private final String namePrefix;
    private final Supplier<List<ProductionSpec>> specsSupplier;
    private final Supplier<List<StepSpec>> stepsSupplier;
    private final Function<IN, String> materialOf;
    private final Function<IN, String> batchIdOf;
    private final BiFunction<ProductionSpec, List<IN>, OUT> producer;
    private final Consumer<OUT> onProduced;
    private final BiConsumer<Exception, List<IN>> onError;

    /** Insumos à espera, agrupados por material. */
    private final Map<String, Deque<IN>> pending = new LinkedHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition arrived = lock.newCondition();

    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private int nextWorkerId = 0;

    public ProductionWorkerPool(String namePrefix,
                                Supplier<List<ProductionSpec>> specsSupplier,
                                Supplier<List<StepSpec>> stepsSupplier,
                                Function<IN, String> materialOf,
                                Function<IN, String> batchIdOf,
                                BiFunction<ProductionSpec, List<IN>, OUT> producer,
                                Consumer<OUT> onProduced,
                                BiConsumer<Exception, List<IN>> onError) {
        this.namePrefix = namePrefix;
        this.specsSupplier = specsSupplier;
        this.stepsSupplier = stepsSupplier;
        this.materialOf = materialOf;
        this.batchIdOf = batchIdOf;
        this.producer = producer;
        this.onProduced = onProduced;
        this.onError = onError;
    }

    /** Entrega um insumo da camada anterior à fila do seu material. */
    public void submit(IN item) {
        lock.lock();
        try {
            pending.computeIfAbsent(materialOf(item), k -> new ArrayDeque<>()).add(item);
            arrived.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /** Total de insumos à espera (todas as filas). */
    public int getQueueSize() {
        lock.lock();
        try {
            return pending.values().stream().mapToInt(Deque::size).sum();
        } finally {
            lock.unlock();
        }
    }

    /** Insumos à espera por material — expõe onde está o gargalo. */
    public Map<String, Integer> getQueueByMaterial() {
        lock.lock();
        try {
            Map<String, Integer> out = new LinkedHashMap<>();
            pending.forEach((material, queue) -> {
                if (!queue.isEmpty()) {
                    out.put(material, queue.size());
                }
            });
            return out;
        } finally {
            lock.unlock();
        }
    }

    public int getWorkerCount() {
        return workers.size();
    }

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
            workers.remove(workers.size() - 1).shutdown();
        }
        log.info("[{}] Pool redimensionado para {} worker(s)", namePrefix, workers.size());
    }

    public synchronized void shutdownAll() {
        workers.forEach(Worker::shutdown);
        workers.clear();
    }

    private String materialOf(IN item) {
        String material = materialOf.apply(item);
        return material == null ? "" : material;
    }

    /** Uma regra satisfeita e os insumos já retirados das filas para a cumprir. */
    private class Batch {
        final ProductionSpec spec;
        final List<IN> items;

        Batch(ProductionSpec spec, List<IN> items) {
            this.spec = spec;
            this.items = items;
        }
    }

    /** Retira os insumos se a regra estiver totalmente satisfeita; senão null. */
    private Batch tryTake(ProductionSpec spec) {
        for (ProductionSpec.Input input : spec.getInputs()) {
            Deque<IN> queue = pending.get(input.getMaterial());
            if (queue == null || queue.size() < input.getQuantity()) {
                return null;
            }
        }
        List<IN> items = new ArrayList<>();
        for (ProductionSpec.Input input : spec.getInputs()) {
            Deque<IN> queue = pending.get(input.getMaterial());
            for (int i = 0; i < input.getQuantity(); i++) {
                items.add(queue.poll());
            }
        }
        return new Batch(spec, items);
    }

    private class Worker extends Thread {
        private volatile boolean active = true;

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
            interrupt();
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

        /**
         * Bloqueia até alguma regra ter todos os seus insumos disponíveis.
         * O tempo-limite na espera faz com que regras acabadas de configurar no
         * portal sejam vistas mesmo sem chegarem novos insumos.
         */
        private Batch awaitBatch() throws InterruptedException {
            while (active) {
                List<ProductionSpec> specs = specsSupplier.get();
                // Sem pipeline configurada não há tempos: a camada não produz.
                // Produzir aqui seria produção instantânea, que o enunciado proíbe.
                boolean hasPipeline = !stepsSupplier.get().isEmpty();

                lock.lock();
                try {
                    if (hasPipeline) {
                        for (ProductionSpec spec : specs) {
                            Batch batch = tryTake(spec);
                            if (batch != null) {
                                return batch;
                            }
                        }
                    }
                    markBlocked();
                    arrived.await(1, TimeUnit.SECONDS);
                } finally {
                    lock.unlock();
                }
            }
            throw new InterruptedException("worker terminado");
        }

        @Override
        public void run() {
            log.info("[{}] iniciado", getName());
            while (active) {
                Batch batch = null;
                try {
                    batch = awaitBatch();

                    batchIds = batch.items.stream().map(batchIdOf).collect(Collectors.joining(","));
                    log.info("[{}] regra '{}' satisfeita: consome {} insumo(s) -> produz {}x {}",
                            getName(), batch.spec.getOutputMaterial(), batch.items.size(),
                            batch.spec.getOutputQuantity(), batch.spec.getOutputMaterial());

                    List<StepSpec> steps = stepsSupplier.get();
                    totalSteps = steps.size();
                    for (int i = 0; i < steps.size(); i++) {
                        StepSpec step = steps.get(i);
                        state = WorkerActivity.State.RUNNING;
                        currentStep = step.getName();
                        stepIndex = i + 1;
                        stepDurationMs = step.getDurationMs();
                        stepStartedAt = System.currentTimeMillis();

                        if (step.getDurationMs() > 0) {
                            Thread.sleep(step.getDurationMs());
                        }
                    }

                    for (int i = 0; i < batch.spec.getOutputQuantity(); i++) {
                        onProduced.accept(producer.apply(batch.spec, batch.items));
                    }
                    markBlocked();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (!active) {
                        log.info("[{}] terminado", getName());
                        return;
                    }
                } catch (Exception ex) {
                    log.error("[{}] falha ao produzir: {}", getName(), ex.getMessage(), ex);
                    if (onError != null && batch != null) {
                        onError.accept(ex, batch.items);
                    }
                    markBlocked();
                }
            }
        }
    }
}
