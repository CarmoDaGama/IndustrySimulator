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
 *   <li>Simula o tempo de produção configurado (lido em tempo real de uma
 *       fonte externa, tipicamente uma tabela de configuração em BD).</li>
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
    private final Supplier<Long> durationMsSupplier;
    private final Function<List<IN>, OUT> producer;
    private final Consumer<OUT> onProduced;
    private final BiConsumer<Exception, List<IN>> onError;

    private final BlockingQueue<IN> inputQueue = new LinkedBlockingQueue<>();
    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private int nextWorkerId = 0;

    public TwoToOneWorkerPool(String namePrefix,
                               int inputsPerOutput,
                               Supplier<Long> durationMsSupplier,
                               Function<List<IN>, OUT> producer,
                               Consumer<OUT> onProduced,
                               BiConsumer<Exception, List<IN>> onError) {
        this.namePrefix = namePrefix;
        this.inputsPerOutput = Math.max(1, inputsPerOutput);
        this.durationMsSupplier = durationMsSupplier;
        this.producer = producer;
        this.onProduced = onProduced;
        this.onError = onError;
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

        Worker(String name) {
            super(name);
            setDaemon(true);
        }

        void shutdown() {
            active = false;
            this.interrupt();
        }

        @Override
        public void run() {
            log.info("[{}] iniciado", getName());
            while (active) {
                List<IN> batch = new ArrayList<>(inputsPerOutput);
                try {
                    // Bloqueio por escassez: espera até existirem 'inputsPerOutput'
                    // unidades disponíveis da camada anterior.
                    for (int i = 0; i < inputsPerOutput; i++) {
                        IN item = inputQueue.take();
                        batch.add(item);
                    }

                    long durationMs = durationMsSupplier.get();
                    if (durationMs > 0) {
                        Thread.sleep(durationMs);
                    }

                    OUT out = producer.apply(batch);
                    onProduced.accept(out);
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
                }
            }
        }
    }
}
