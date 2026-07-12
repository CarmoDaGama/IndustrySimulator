package com.industry.simulator.processing.service;

import com.industry.simulator.common.events.ProcessingCompletedEvent;
import com.industry.simulator.common.events.RawMaterialProducedEvent;
import com.industry.simulator.common.model.Component;
import com.industry.simulator.common.worker.StepSpec;
import com.industry.simulator.common.worker.TwoToOneWorkerPool;
import com.industry.simulator.common.worker.WorkerActivity;
import com.industry.simulator.processing.entity.ProcessedMaterial;
import com.industry.simulator.processing.entity.WorkerPoolConfig;
import com.industry.simulator.processing.kafka.ProcessingProducer;
import com.industry.simulator.processing.repository.PipelineStepRepository;
import com.industry.simulator.processing.repository.ProcessedMaterialRepository;
import com.industry.simulator.processing.repository.WorkerPoolConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Camada de Processamento (Camada 2). Os workers desta pool cumprem a
 * "Regra de Consumo da Cadeia (2:1)": cada unidade produzida consome, no
 * mínimo, 2 unidades de matéria-prima da camada anterior. Enquanto não
 * existirem 2 unidades disponíveis, o worker bloqueia automaticamente
 * (ver {@link TwoToOneWorkerPool}).
 */
@Service
public class ProcessingWorkerPoolService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingWorkerPoolService.class);
    private static final int INPUTS_PER_OUTPUT = 2;

    @Autowired
    private ProcessedMaterialRepository repository;

    @Autowired
    private ProcessingProducer producer;

    @Autowired
    private PipelineStepRepository pipelineRepository;

    @Autowired
    private WorkerPoolConfigRepository workerPoolConfigRepository;

    private TwoToOneWorkerPool<RawMaterialProducedEvent, ProcessingCompletedEvent> pool;

    @PostConstruct
    public void init() {
        pool = new TwoToOneWorkerPool<>(
                "processing",
                INPUTS_PER_OUTPUT,
                this::currentSteps,
                this::produce,
                producer::publishProcessingCompleted,
                this::handleError,
                RawMaterialProducedEvent::getBatchId
        );
        pool.resize(currentWorkerCount());
    }

    /** Estado ao vivo de cada Worker (etapa em execução) para o portal. */
    public List<WorkerActivity> getActivities() {
        return pool.getActivities();
    }

    public void submit(RawMaterialProducedEvent event) {
        pool.submit(event);
    }

    public int getWorkerCount() {
        return pool.getWorkerCount();
    }

    public int getQueueSize() {
        return pool.getQueueSize();
    }

    public synchronized int resize(int workerCount) {
        WorkerPoolConfig config = workerPoolConfigRepository.findById(1L)
                .orElseGet(WorkerPoolConfig::new);
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
     * Etapas da pipeline lidas em tempo real da BD (portal de configurações).
     * Os Workers executam-nas uma a uma, tornando cada etapa observável.
     */
    private List<StepSpec> currentSteps() {
        List<com.industry.simulator.processing.entity.PipelineStep> steps =
                pipelineRepository.findAllByIsActiveOrderByStepOrderAsc(true);
        if (steps.isEmpty()) {
            return List.of(new StepSpec("PROCESSING", 2000L));
        }
        return steps.stream()
                .map(s -> new StepSpec(s.getStepName(), s.getDurationMs()))
                .collect(Collectors.toList());
    }

    private long currentDurationMs() {
        return currentSteps().stream().mapToLong(StepSpec::getDurationMs).sum();
    }

    private ProcessingCompletedEvent produce(List<RawMaterialProducedEvent> batch) {
        String outputBatchId = UUID.randomUUID().toString();
        String sourceBatchIds = batch.stream().map(RawMaterialProducedEvent::getBatchId)
                .collect(Collectors.joining(","));
        RawMaterialProducedEvent first = batch.get(0);
        double totalQuantity = batch.stream().mapToDouble(RawMaterialProducedEvent::getQuantity).sum();
        long durationMs = currentDurationMs();

        log.info("{} | processing-service | Consumindo {} unidades ({}) para produzir 1 unidade processada",
                outputBatchId, batch.size(), sourceBatchIds);

        ProcessedMaterial processed = new ProcessedMaterial();
        processed.setBatchId(outputBatchId);
        processed.setSourceBatchIds(sourceBatchIds);
        processed.setMaterialName(first.getMaterial().getName());
        processed.setMaterialType(first.getMaterial().getType());
        processed.setQuantity(totalQuantity);
        processed.setUnit(first.getUnit());
        processed.setProcessingType(determineProcessingType(first.getMaterial().getType()));
        processed.setProcessingDurationMs(durationMs);
        processed.setPurpose(first.getPurpose());
        processed.setSuccess(true);
        processed.setCreatedAt(LocalDateTime.now());
        processed.setCompletedAt(LocalDateTime.now());
        repository.save(processed);

        Component processedComponent = Component.builder()
                .id("proc-" + outputBatchId.substring(0, 8))
                .name("Processed " + first.getMaterial().getName())
                .type("PROCESSED_MATERIAL")
                .quantity(totalQuantity)
                .unit(first.getUnit())
                .batchId(outputBatchId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .producer(Component.Producer.builder()
                        .service("processing-service")
                        .factory("refining-plant-beta")
                        .build())
                .purpose(first.getMaterial().getPurpose())
                .components(batch.stream().map(RawMaterialProducedEvent::getMaterial).collect(Collectors.toList()))
                .build();

        return ProcessingCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MATERIAL_PROCESSED")
                .batchId(outputBatchId)
                .processedMaterial(processedComponent)
                .processingType(processed.getProcessingType())
                .processingDurationMs(durationMs)
                .timestamp(LocalDateTime.now())
                .purpose(first.getPurpose())
                .success(true)
                .build();
    }

    private String determineProcessingType(String materialType) {
        String lower = materialType == null ? "" : materialType.toLowerCase();
        if ("steel".equals(lower) || "aluminum".equals(lower)) {
            return "melting";
        } else if ("plastic".equals(lower) || "rubber".equals(lower)) {
            return "molding";
        } else if ("wood".equals(lower)) {
            return "cutting";
        } else {
            return "processing";
        }
    }

    private void handleError(Exception ex, List<RawMaterialProducedEvent> batch) {
        for (RawMaterialProducedEvent event : batch) {
            try {
                ProcessedMaterial errorMaterial = new ProcessedMaterial();
                errorMaterial.setBatchId(UUID.randomUUID().toString());
                errorMaterial.setSourceBatchIds(event.getBatchId());
                errorMaterial.setMaterialName(event.getMaterial().getName());
                errorMaterial.setMaterialType(event.getMaterial().getType());
                errorMaterial.setQuantity(event.getQuantity());
                errorMaterial.setUnit(event.getUnit());
                errorMaterial.setProcessingType("unknown");
                errorMaterial.setProcessingDurationMs(0);
                errorMaterial.setPurpose(event.getPurpose());
                errorMaterial.setSuccess(false);
                errorMaterial.setErrorMessage(ex.getMessage());
                errorMaterial.setCreatedAt(LocalDateTime.now());
                errorMaterial.setCompletedAt(LocalDateTime.now());
                repository.save(errorMaterial);
            } catch (Exception persistError) {
                log.error("{} | processing-service | Erro ao gravar falha de processamento", event.getBatchId(), persistError);
            }
        }
    }
}
