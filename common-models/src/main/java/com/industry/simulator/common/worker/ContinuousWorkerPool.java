package com.industry.simulator.common.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Pool de Workers genérico para camadas sem dependência da camada anterior
 * (Camada 1 - Extracção, e a simulação de pedidos da Camada 6 - Mercado).
 * Cada Worker é uma Thread que corre continuamente e autonomamente:
 * produz uma unidade sempre que existir configuração disponível, ou
 * aguarda (bloqueia) caso ainda não exista nenhuma configuração activa.
 *
 * @param <OUT> tipo do item produzido em cada ciclo
 */
public class ContinuousWorkerPool<OUT> {

    private static final Logger log = LoggerFactory.getLogger(ContinuousWorkerPool.class);
    private static final long IDLE_RETRY_MS = 1000L;

    /** Como {@link java.util.function.Supplier}, mas permite propagar InterruptedException. */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws InterruptedException;
    }

    private final String namePrefix;
    private final ThrowingSupplier<OUT> producer;
    private final Consumer<OUT> onProduced;
    private final Consumer<Exception> onError;

    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private int nextWorkerId = 0;

    public ContinuousWorkerPool(String namePrefix,
                                 ThrowingSupplier<OUT> producer,
                                 Consumer<OUT> onProduced,
                                 Consumer<Exception> onError) {
        this.namePrefix = namePrefix;
        this.producer = producer;
        this.onProduced = onProduced;
        this.onError = onError;
    }

    public int getWorkerCount() {
        return workers.size();
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
                try {
                    OUT out = producer.get();
                    if (out == null) {
                        // Sem configuração/recursos disponíveis: aguarda e tenta de novo.
                        Thread.sleep(IDLE_RETRY_MS);
                        continue;
                    }
                    onProduced.accept(out);
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
                }
            }
        }
    }
}
