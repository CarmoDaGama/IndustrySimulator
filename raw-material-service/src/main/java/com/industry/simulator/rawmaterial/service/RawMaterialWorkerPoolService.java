package com.industry.simulator.rawmaterial.service;

import com.industry.simulator.common.events.RawMaterialProducedEvent;
import com.industry.simulator.common.model.Component;
import com.industry.simulator.common.worker.ContinuousWorkerPool;
import com.industry.simulator.common.worker.StepSpec;
import com.industry.simulator.common.worker.WorkerActivity;
import com.industry.simulator.rawmaterial.entity.ExtractionConfig;
import com.industry.simulator.rawmaterial.entity.PipelineStep;
import com.industry.simulator.rawmaterial.entity.RawMaterial;
import com.industry.simulator.rawmaterial.entity.WorkerPoolConfig;
import com.industry.simulator.rawmaterial.kafka.RawMaterialProducer;
import com.industry.simulator.rawmaterial.repository.ExtractionConfigRepository;
import com.industry.simulator.rawmaterial.repository.PipelineStepRepository;
import com.industry.simulator.rawmaterial.repository.RawMaterialRepository;
import com.industry.simulator.rawmaterial.repository.WorkerPoolConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Camada 1 (Extracção). Ao contrário das restantes camadas, não depende de
 * nenhuma camada anterior: os Workers extraem continuamente e
 * autonomamente, escolhendo entre as configurações activas em BD
 * ({@link ExtractionConfig}). Se ainda não existir nenhuma configuração
 * activa, os Workers ficam à espera (ver {@link ContinuousWorkerPool}).
 */
@Service
public class RawMaterialWorkerPoolService {

    private static final Logger log = LoggerFactory.getLogger(RawMaterialWorkerPoolService.class);

    @Autowired
    private RawMaterialRepository repository;

    @Autowired
    private RawMaterialProducer producer;

    @Autowired
    private ExtractionConfigRepository extractionConfigRepository;

    @Autowired
    private WorkerPoolConfigRepository workerPoolConfigRepository;

    @Autowired
    private PipelineStepRepository pipelineRepository;

    private ContinuousWorkerPool<RawMaterialProducedEvent> pool;

    @PostConstruct
    public void init() {
        pool = new ContinuousWorkerPool<>(
                "extraction",
                this::nextCycle,
                producer::publishRawMaterialProduced,
                ex -> log.error("Falha no ciclo de extracção: {}", ex.getMessage(), ex)
        );
        pool.resize(currentWorkerCount());
    }

    public int getWorkerCount() { return pool.getWorkerCount(); }

    /** Estado ao vivo de cada Worker (etapa em execução) para o portal. */
    public List<WorkerActivity> getActivities() {
        return pool.getActivities();
    }

    public synchronized int resize(int workerCount) {
        WorkerPoolConfig config = workerPoolConfigRepository.findById(1L).orElseGet(WorkerPoolConfig::new);
        config.setWorkerCount(workerCount);
        workerPoolConfigRepository.save(config);
        pool.resize(workerCount);
        return pool.getWorkerCount();
    }

    private int currentWorkerCount() {
        return workerPoolConfigRepository.findById(1L)
                .map(WorkerPoolConfig::getWorkerCount)
                .orElseGet(() -> {
                    WorkerPoolConfig config = new WorkerPoolConfig();
                    workerPoolConfigRepository.save(config);
                    return config.getWorkerCount();
                });
    }

    /**
     * Prepara o próximo ciclo de extracção para um recurso activo escolhido
     * aleatoriamente. Tal como nas restantes camadas, os tempos vêm da pipeline
     * configurada no portal — se não houver pipeline (ou nenhum recurso activo),
     * devolve {@code null} e os Workers ficam à espera, sem produzir.
     */
    private ContinuousWorkerPool.Cycle<RawMaterialProducedEvent> nextCycle() {
        List<ExtractionConfig> configs = extractionConfigRepository.findByActiveTrue();
        List<StepSpec> steps = currentSteps();
        if (configs.isEmpty() || steps.isEmpty()) {
            return null;
        }
        ExtractionConfig config = configs.get(ThreadLocalRandom.current().nextInt(configs.size()));
        String batchId = UUID.randomUUID().toString();

        return new ContinuousWorkerPool.Cycle<>(steps, batchId, () -> buildEvent(config, batchId));
    }

    /**
     * Etapas da extracção, vindas da pipeline configurada no portal (ex.:
     * EXTRACTION, INITIAL_PROCESSING, PACKAGING_FOR_TRANSPORT). Não há tempos
     * fixos no código: sem pipeline, a camada não produz.
     */
    private List<StepSpec> currentSteps() {
        return pipelineRepository.findAllByIsActiveOrderByStepOrderAsc(true).stream()
                .map(s -> new StepSpec(s.getStepName(), s.getDurationMs()))
                .collect(Collectors.toList());
    }

    /** Persiste o material extraído e constrói o evento (após as etapas correrem). */
    private RawMaterialProducedEvent buildEvent(ExtractionConfig config, String batchId) {
        RawMaterial material = new RawMaterial();
        material.setMaterialName(config.getMaterialName());
        material.setMaterialType(config.getMaterialType());
        material.setQuantity(config.getQuantityPerCycle());
        material.setUnit(config.getUnit());
        material.setBatchId(batchId);
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());
        repository.save(material);

        Component component = Component.builder()
                .id(batchId)
                .name(config.getMaterialName())
                .type("RAW_MATERIAL")
                .quantity(config.getQuantityPerCycle())
                .unit(config.getUnit())
                .batchId(batchId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .producer(Component.Producer.builder()
                        .service("raw-material-service")
                        .factory(config.getFactory())
                        .build())
                .purpose(Component.Purpose.builder()
                        .targetProduct(config.getTargetProduct())
                        .targetComponent(config.getTargetComponent())
                        .description(config.getDescription())
                        .build())
                .build();

        log.info("{} | raw-material-service | Extracção e transporte concluídos", batchId);

        return RawMaterialProducedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("RAW_MATERIAL_EXTRACTED")
                .timestamp(System.currentTimeMillis() / 1000)
                .payload(component)
                .build();
    }
}
