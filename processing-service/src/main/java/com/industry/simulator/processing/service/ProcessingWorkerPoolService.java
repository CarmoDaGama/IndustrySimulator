package com.industry.simulator.processing.service;

import com.industry.simulator.common.events.ProcessingCompletedEvent;
import com.industry.simulator.common.events.RawMaterialProducedEvent;
import com.industry.simulator.common.model.Component;
import com.industry.simulator.common.worker.ProductionSpec;
import com.industry.simulator.common.worker.ProductionWorkerPool;
import com.industry.simulator.common.worker.StepSpec;
import com.industry.simulator.common.worker.WorkerActivity;
import com.industry.simulator.processing.entity.PipelineStep;
import com.industry.simulator.processing.entity.ProcessedMaterial;
import com.industry.simulator.processing.entity.ProductionRule;
import com.industry.simulator.processing.entity.WorkerPoolConfig;
import com.industry.simulator.processing.kafka.ProcessingProducer;
import com.industry.simulator.processing.repository.PipelineStepRepository;
import com.industry.simulator.processing.repository.ProcessedMaterialRepository;
import com.industry.simulator.processing.repository.ProductionRuleRepository;
import com.industry.simulator.processing.repository.WorkerPoolConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Camada 2 (Processamento). Os Workers são guiados pelas regras de produção
 * configuradas no portal: cada regra diz que material consumir, em que
 * quantidade, e que material refinado produzir. Sem regras, os Workers ficam
 * bloqueados — a produção só arranca quando houver configuração.
 */
@Service
public class ProcessingWorkerPoolService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingWorkerPoolService.class);
    private static final String SERVICE = "processing-service";
    private static final String DEFAULT_FACTORY = "refining-plant-beta";

    @Autowired
    private ProcessedMaterialRepository repository;

    @Autowired
    private ProcessingProducer producer;

    @Autowired
    private PipelineStepRepository pipelineRepository;

    @Autowired
    private ProductionRuleRepository ruleRepository;

    @Autowired
    private WorkerPoolConfigRepository workerPoolConfigRepository;

    private ProductionWorkerPool<RawMaterialProducedEvent, ProcessingCompletedEvent> pool;

    @PostConstruct
    public void init() {
        pool = new ProductionWorkerPool<>(
                "processing",
                this::currentSpecs,
                this::currentSteps,
                event -> event.getPayload().getName(),
                event -> event.getPayload().getBatchId(),
                this::produce,
                producer::publishProcessingCompleted,
                this::handleError
        );
        pool.resize(currentWorkerCount());
    }

    public void submit(RawMaterialProducedEvent event) {
        pool.submit(event);
    }

    public int getWorkerCount() { return pool.getWorkerCount(); }
    public int getQueueSize() { return pool.getQueueSize(); }
    public Map<String, Integer> getQueueByMaterial() { return pool.getQueueByMaterial(); }
    public List<WorkerActivity> getActivities() { return pool.getActivities(); }

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

    /** Regras de transformação lidas em tempo real da BD (portal). */
    private List<ProductionSpec> currentSpecs() {
        return ruleRepository.findByActiveTrue().stream()
                .map(this::toSpec)
                .collect(Collectors.toList());
    }

    private ProductionSpec toSpec(ProductionRule rule) {
        List<ProductionSpec.Input> inputs = rule.getInputs().stream()
                .map(i -> new ProductionSpec.Input(i.getInputMaterial(), i.getInputQuantity()))
                .collect(Collectors.toList());
        return new ProductionSpec(
                rule.getOutputMaterial(),
                rule.getOutputType() != null ? rule.getOutputType() : "TRANSFORMED_MATERIAL",
                rule.getOutputQuantity(),
                rule.getFactory() != null ? rule.getFactory() : DEFAULT_FACTORY,
                rule.getTargetProduct(), rule.getTargetComponent(), rule.getDescription(),
                inputs
        );
    }

    /** Etapas da pipeline, executadas uma a uma pelos Workers. */
    private List<StepSpec> currentSteps() {
        List<PipelineStep> steps = pipelineRepository.findAllByIsActiveOrderByStepOrderAsc(true);
        if (steps.isEmpty()) {
            return List.of(new StepSpec("REFINING", 2000L));
        }
        return steps.stream()
                .map(s -> new StepSpec(s.getStepName(), s.getDurationMs()))
                .collect(Collectors.toList());
    }

    private ProcessingCompletedEvent produce(ProductionSpec spec, List<RawMaterialProducedEvent> consumed) {
        String batchId = UUID.randomUUID().toString();
        String sourceBatchIds = consumed.stream()
                .map(e -> e.getPayload().getBatchId())
                .collect(Collectors.joining(","));
        long durationMs = currentSteps().stream().mapToLong(StepSpec::getDurationMs).sum();

        log.info("{} | {} | {} -> {} ({} insumos consumidos)",
                batchId, SERVICE, sourceBatchIds, spec.getOutputMaterial(), consumed.size());

        ProcessedMaterial processed = new ProcessedMaterial();
        processed.setBatchId(batchId);
        processed.setSourceBatchIds(sourceBatchIds);
        processed.setMaterialName(spec.getOutputMaterial());
        processed.setMaterialType(spec.getOutputType());
        processed.setQuantity(1);
        processed.setUnit(consumed.get(0).getPayload().getUnit());
        processed.setProcessingType(spec.getOutputType());
        processed.setProcessingDurationMs(durationMs);
        processed.setPurpose(spec.getTargetProduct());
        processed.setSuccess(true);
        processed.setCreatedAt(LocalDateTime.now());
        processed.setCompletedAt(LocalDateTime.now());
        repository.save(processed);

        // Nó da árvore: os insumos consumidos ficam em components[]
        Component payload = Component.builder()
                .id(batchId)
                .name(spec.getOutputMaterial())
                .type(spec.getOutputType())
                .quantity(1)
                .unit(consumed.get(0).getPayload().getUnit())
                .batchId(batchId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .producer(Component.Producer.builder()
                        .service(SERVICE)
                        .factory(spec.getFactory())
                        .build())
                .purpose(Component.Purpose.builder()
                        .targetProduct(spec.getTargetProduct())
                        .targetComponent(spec.getTargetComponent())
                        .description(spec.getDescription())
                        .build())
                .components(consumed.stream().map(RawMaterialProducedEvent::getPayload).collect(Collectors.toList()))
                .build();

        return ProcessingCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("MATERIAL_PROCESSED")
                .timestamp(System.currentTimeMillis() / 1000)
                .payload(payload)
                .build();
    }

    private void handleError(Exception ex, List<RawMaterialProducedEvent> consumed) {
        for (RawMaterialProducedEvent event : consumed) {
            try {
                ProcessedMaterial errorMaterial = new ProcessedMaterial();
                errorMaterial.setBatchId(UUID.randomUUID().toString());
                errorMaterial.setSourceBatchIds(event.getPayload().getBatchId());
                errorMaterial.setMaterialName(event.getPayload().getName());
                errorMaterial.setMaterialType(event.getPayload().getType());
                errorMaterial.setQuantity(event.getPayload().getQuantity());
                errorMaterial.setUnit(event.getPayload().getUnit());
                errorMaterial.setProcessingType("unknown");
                errorMaterial.setProcessingDurationMs(0);
                errorMaterial.setSuccess(false);
                errorMaterial.setErrorMessage(ex.getMessage());
                errorMaterial.setCreatedAt(LocalDateTime.now());
                errorMaterial.setCompletedAt(LocalDateTime.now());
                repository.save(errorMaterial);
            } catch (Exception persistError) {
                log.error("{} | {} | Erro ao gravar falha", event.getEventId(), SERVICE, persistError);
            }
        }
    }
}
