package com.industry.simulator.assembly.service;

import com.industry.simulator.assembly.dto.MarketOrderRequest;
import com.industry.simulator.assembly.entity.CustomerSimulatorConfig;
import com.industry.simulator.assembly.repository.CustomerSimulatorConfigRepository;
import com.industry.simulator.common.worker.ContinuousWorkerPool;
import com.industry.simulator.common.worker.StepSpec;
import com.industry.simulator.common.worker.WorkerActivity;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Camada 6 (Mercado) — simulação automática e assíncrona de clientes
 * fictícios, como o enunciado exige. Cada cliente é uma Thread que, em ciclo:
 * "pensa" durante um tempo configurável e depois encomenda um dos produtos
 * que a fábrica sabe montar (vindos das regras de produção da Camada 4).
 *
 * Se o produto não existir em stock, o pedido fica PENDENTE e é desbloqueado
 * automaticamente quando o inventário for reposto.
 */
@Service
public class CustomerSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(CustomerSimulatorService.class);

    private static final List<String> NOMES = List.of(
            "Ana Cardoso", "Bruno Katendi", "Carla Mendes", "Domingos Kiala",
            "Eunice Bastos", "Fernando Luvualu", "Gabriela Neto", "Hélder Muanza"
    );

    @Autowired
    private MarketOrderService marketOrderService;

    @Autowired
    private AssemblyWorkerPoolService assemblyWorkerPoolService;

    @Autowired
    private CustomerSimulatorConfigRepository configRepository;

    private ContinuousWorkerPool<Void> pool;

    @PostConstruct
    public void init() {
        pool = new ContinuousWorkerPool<>(
                "cliente",
                this::nextPurchase,
                ignored -> { },
                ex -> log.error("Falha na simulação de cliente: {}", ex.getMessage(), ex)
        );
        pool.resize(config().getCustomerCount());
    }

    public CustomerSimulatorConfig config() {
        return configRepository.findById(1L).orElseGet(() -> {
            CustomerSimulatorConfig fresh = new CustomerSimulatorConfig();
            return configRepository.save(fresh);
        });
    }

    public int getCustomerCount() {
        return pool.getWorkerCount();
    }

    public List<WorkerActivity> getActivities() {
        return pool.getActivities();
    }

    /** Actualiza os parâmetros e aplica de imediato o nº de clientes activos. */
    public synchronized CustomerSimulatorConfig update(CustomerSimulatorConfig changes) {
        CustomerSimulatorConfig config = config();
        config.setCustomerCount(Math.max(0, changes.getCustomerCount()));
        config.setThinkTimeMs(Math.max(500, changes.getThinkTimeMs()));
        config.setMinQuantity(Math.max(1, changes.getMinQuantity()));
        config.setMaxQuantity(Math.max(config.getMinQuantity(), changes.getMaxQuantity()));
        configRepository.save(config);
        pool.resize(config.getCustomerCount());
        return config;
    }

    /**
     * Um ciclo de compra. Devolve {@code null} enquanto a fábrica não souber
     * montar nenhum produto (sem regras na Camada 4), deixando os clientes à
     * espera em vez de encomendarem algo inexistente.
     */
    private ContinuousWorkerPool.Cycle<Void> nextPurchase() {
        List<String> produtos = assemblyWorkerPoolService.getProducibleProducts();
        if (produtos.isEmpty()) {
            return null;
        }

        CustomerSimulatorConfig config = config();
        String produto = produtos.get(ThreadLocalRandom.current().nextInt(produtos.size()));
        String cliente = NOMES.get(ThreadLocalRandom.current().nextInt(NOMES.size()));
        int quantidade = ThreadLocalRandom.current()
                .nextInt(config.getMinQuantity(), config.getMaxQuantity() + 1);

        // O "tempo a decidir" é uma etapa da pipeline: fica visível no monitor.
        List<StepSpec> steps = List.of(new StepSpec("DECISAO_DE_COMPRA", config.getThinkTimeMs()));

        return new ContinuousWorkerPool.Cycle<>(steps, UUID.randomUUID().toString(), () -> {
            MarketOrderRequest request = new MarketOrderRequest();
            request.setProductType(produto);
            request.setQuantity(quantidade);
            request.setCustomerName(cliente);
            request.setPriority(ThreadLocalRandom.current().nextInt(1, 4));
            request.setBomVersion("v1.0.0");
            request.setRequiredDeliveryDate(LocalDateTime.now().plusDays(7));

            marketOrderService.createOrder(request);
            log.info("cliente-simulado | {} encomendou {}x {}", cliente, quantidade, produto);
            return null;
        });
    }
}
