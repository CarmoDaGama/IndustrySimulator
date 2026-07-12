package com.industry.simulator.component.service;

import com.industry.simulator.common.events.ComponentAssembledEvent;
import com.industry.simulator.common.events.ProcessingCompletedEvent;
import com.industry.simulator.common.worker.StepSpec;
import com.industry.simulator.common.worker.TwoToOneWorkerPool;
import com.industry.simulator.common.worker.WorkerActivity;
import com.industry.simulator.component.entity.Component;
import com.industry.simulator.component.entity.PipelineStep;
import com.industry.simulator.component.entity.WorkerPoolConfig;
import com.industry.simulator.component.kafka.ComponentProducer;
import com.industry.simulator.component.repository.ComponentRepository;
import com.industry.simulator.component.repository.PipelineStepRepository;
import com.industry.simulator.component.repository.WorkerPoolConfigRepository;
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
 * Camada de Componentes (Camada 3). Cumpre a regra 2:1: consome 2 unidades
 * processadas para produzir 1 componente, bloqueando automaticamente
 * quando a camada anterior não repõe stock a tempo.
 */
@Service
public class ComponentWorkerPoolService {

    private static final Logger log = LoggerFactory.getLogger(ComponentWorkerPoolService.class);
    private static final int INPUTS_PER_OUTPUT = 2;
    private static final long DEFAULT_DURATION_MS = 3000L;

    @Autowired
    private ComponentRepository repository;

    @Autowired
    private ComponentProducer producer;

    @Autowired
    private BOMValidationService bomValidationService;

    @Autowired
    private PipelineStepRepository pipelineRepository;

    @Autowired
    private WorkerPoolConfigRepository workerPoolConfigRepository;

    private TwoToOneWorkerPool<ProcessingCompletedEvent, ComponentAssembledEvent> pool;

    @PostConstruct
    public void init() {
        pool = new TwoToOneWorkerPool<>(
                "component",
                INPUTS_PER_OUTPUT,
                this::currentSteps,
                this::produce,
                producer::publishComponentAssembled,
                this::handleError,
                ProcessingCompletedEvent::getBatchId
        );
        pool.resize(currentWorkerCount());
    }

    /** Estado ao vivo de cada Worker (etapa em execução) para o portal. */
    public List<WorkerActivity> getActivities() {
        return pool.getActivities();
    }

    public void submit(ProcessingCompletedEvent event) {
        pool.submit(event);
    }

    public int getWorkerCount() { return pool.getWorkerCount(); }
    public int getQueueSize() { return pool.getQueueSize(); }

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

    /** Etapas lidas em tempo real da BD; executadas uma a uma pelos Workers. */
    private List<StepSpec> currentSteps() {
        List<PipelineStep> steps = pipelineRepository.findAllByIsActiveOrderByStepOrderAsc(true);
        if (steps.isEmpty()) {
            return List.of(new StepSpec("ASSEMBLY", DEFAULT_DURATION_MS));
        }
        return steps.stream()
                .map(s -> new StepSpec(s.getStepName(), s.getDurationMs()))
                .collect(Collectors.toList());
    }

    private long currentDurationMs() {
        return currentSteps().stream().mapToLong(StepSpec::getDurationMs).sum();
    }

    private ComponentAssembledEvent produce(List<ProcessingCompletedEvent> batch) {
        String outputBatchId = UUID.randomUUID().toString();
        String sourceBatchIds = batch.stream().map(ProcessingCompletedEvent::getBatchId)
                .collect(Collectors.joining(","));
        ProcessingCompletedEvent first = batch.get(0);
        long durationMs = currentDurationMs();

        log.info("{} | component-service | Consumindo {} unidades processadas ({}) para produzir 1 componente",
                outputBatchId, batch.size(), sourceBatchIds);

        boolean compatible = bomValidationService.validateComponentCompatibility(
                first.getProcessedMaterial().getType());
        String compatibilityNotes = compatible
                ? "Material compatible with BOM requirements"
                : "Material may have compatibility issues with BOM";

        Component entity = new Component();
        entity.setBatchId(outputBatchId);
        entity.setSourceBatchIds(sourceBatchIds);
        entity.setComponentName(first.getProcessedMaterial().getName());
        entity.setComponentType(first.getProcessedMaterial().getType());
        entity.setQuantity(1.0);
        entity.setUnit("unit");
        entity.setProcessingType(first.getProcessingType());
        entity.setBomValidated(true);
        entity.setCompatible(compatible);
        entity.setCompatibilityNotes(compatibilityNotes);
        entity.setPurpose(first.getPurpose());
        entity.setAssembled(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setAssembledAt(LocalDateTime.now());
        repository.save(entity);

        com.industry.simulator.common.model.Component finalPart = com.industry.simulator.common.model.Component.builder()
                .id("comp-" + outputBatchId.substring(0, 8))
                .name("Industrial Part: " + first.getProcessedMaterial().getName())
                .type("COMPONENT")
                .quantity(1.0)
                .unit("unit")
                .batchId(outputBatchId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .producer(com.industry.simulator.common.model.Component.Producer.builder()
                        .service("component-service")
                        .factory("manufacturing-hub-gamma")
                        .build())
                .purpose(first.getProcessedMaterial().getPurpose())
                .compatibleForAssembly(compatible)
                .components(batch.stream().map(ProcessingCompletedEvent::getProcessedMaterial).collect(Collectors.toList()))
                .build();

        return ComponentAssembledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("COMPONENT_CREATED")
                .batchId(outputBatchId)
                .finalComponent(finalPart)
                .assemblyDurationMs(durationMs)
                .timestamp(LocalDateTime.now())
                .purpose(first.getPurpose())
                .success(true)
                .build();
    }

    private void handleError(Exception ex, List<ProcessingCompletedEvent> batch) {
        for (ProcessingCompletedEvent event : batch) {
            try {
                Component errorComponent = new Component();
                errorComponent.setBatchId(UUID.randomUUID().toString());
                errorComponent.setSourceBatchIds(event.getBatchId());
                errorComponent.setComponentName(event.getProcessedMaterial().getName());
                errorComponent.setComponentType(event.getProcessedMaterial().getType());
                errorComponent.setQuantity(1.0);
                errorComponent.setUnit("unit");
                errorComponent.setProcessingType(event.getProcessingType());
                errorComponent.setBomValidated(false);
                errorComponent.setCompatible(false);
                errorComponent.setCompatibilityNotes("Assembly failed: " + ex.getMessage());
                errorComponent.setPurpose(event.getPurpose());
                errorComponent.setAssembled(false);
                errorComponent.setCreatedAt(LocalDateTime.now());
                errorComponent.setAssembledAt(LocalDateTime.now());
                repository.save(errorComponent);
            } catch (Exception persistError) {
                log.error("{} | component-service | Erro ao gravar falha de assembly", event.getBatchId(), persistError);
            }
        }
    }
}
