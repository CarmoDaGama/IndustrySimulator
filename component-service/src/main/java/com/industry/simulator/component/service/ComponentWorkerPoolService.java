package com.industry.simulator.component.service;

import com.industry.simulator.common.events.ComponentAssembledEvent;
import com.industry.simulator.common.events.ProcessingCompletedEvent;
import com.industry.simulator.common.worker.ProductionSpec;
import com.industry.simulator.common.worker.ProductionWorkerPool;
import com.industry.simulator.common.worker.StepSpec;
import com.industry.simulator.common.worker.WorkerActivity;
import com.industry.simulator.component.entity.Component;
import com.industry.simulator.component.entity.PipelineStep;
import com.industry.simulator.component.entity.ProductionRule;
import com.industry.simulator.component.entity.WorkerPoolConfig;
import com.industry.simulator.component.kafka.ComponentProducer;
import com.industry.simulator.component.repository.ComponentRepository;
import com.industry.simulator.component.repository.PipelineStepRepository;
import com.industry.simulator.component.repository.ProductionRuleRepository;
import com.industry.simulator.component.repository.WorkerPoolConfigRepository;
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
 * Camada 3 (Componentes). Guiada pelas regras de produção do portal: consome
 * os materiais refinados indicados e produz a peça definida na regra.
 */
@Service
public class ComponentWorkerPoolService {

    private static final Logger log = LoggerFactory.getLogger(ComponentWorkerPoolService.class);
    private static final String SERVICE = "component-service";
    private static final String DEFAULT_FACTORY = "manufacturing-hub-gamma";

    @Autowired
    private ComponentRepository repository;

    @Autowired
    private ComponentProducer producer;

    @Autowired
    private BOMValidationService bomValidationService;

    @Autowired
    private PipelineStepRepository pipelineRepository;

    @Autowired
    private ProductionRuleRepository ruleRepository;

    @Autowired
    private WorkerPoolConfigRepository workerPoolConfigRepository;

    private ProductionWorkerPool<ProcessingCompletedEvent, ComponentAssembledEvent> pool;

    @PostConstruct
    public void init() {
        pool = new ProductionWorkerPool<>(
                "component",
                this::currentSpecs,
                this::currentSteps,
                event -> event.getPayload().getName(),
                event -> event.getPayload().getBatchId(),
                this::produce,
                producer::publishComponentAssembled,
                this::handleError
        );
        pool.resize(currentWorkerCount());
    }

    public void submit(ProcessingCompletedEvent event) {
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

    private List<ProductionSpec> currentSpecs() {
        return ruleRepository.findByActiveTrue().stream().map(this::toSpec).collect(Collectors.toList());
    }

    private ProductionSpec toSpec(ProductionRule rule) {
        List<ProductionSpec.Input> inputs = rule.getInputs().stream()
                .map(i -> new ProductionSpec.Input(i.getInputMaterial(), i.getInputQuantity()))
                .collect(Collectors.toList());
        return new ProductionSpec(
                rule.getOutputMaterial(),
                rule.getOutputType() != null ? rule.getOutputType() : "COMPONENT",
                rule.getOutputQuantity(),
                rule.getFactory() != null ? rule.getFactory() : DEFAULT_FACTORY,
                rule.getTargetProduct(), rule.getTargetComponent(), rule.getDescription(),
                inputs
        );
    }

    private List<StepSpec> currentSteps() {
        List<PipelineStep> steps = pipelineRepository.findAllByIsActiveOrderByStepOrderAsc(true);
        return steps.stream()
                .map(s -> new StepSpec(s.getStepName(), s.getDurationMs()))
                .collect(Collectors.toList());
    }

    private ComponentAssembledEvent produce(ProductionSpec spec, List<ProcessingCompletedEvent> consumed) {
        String batchId = UUID.randomUUID().toString();
        String sourceBatchIds = consumed.stream()
                .map(e -> e.getPayload().getBatchId())
                .collect(Collectors.joining(","));

        // Validação de BOM: todos os insumos consumidos são compatíveis?
        boolean compatible = consumed.stream()
                .allMatch(e -> bomValidationService.validateComponentCompatibility(e.getPayload().getName()));
        String notes = compatible
                ? "Todos os insumos compatíveis com as regras de BOM"
                : "Pelo menos um insumo não consta das regras de compatibilidade";

        log.info("{} | {} | {} -> {} ({} insumos, compativel={})",
                batchId, SERVICE, sourceBatchIds, spec.getOutputMaterial(), consumed.size(), compatible);

        Component entity = new Component();
        entity.setBatchId(batchId);
        entity.setSourceBatchIds(sourceBatchIds);
        entity.setComponentName(spec.getOutputMaterial());
        entity.setComponentType(spec.getOutputType());
        entity.setQuantity(1.0);
        entity.setUnit("unit");
        entity.setProcessingType(spec.getOutputType());
        entity.setBomValidated(true);
        entity.setCompatible(compatible);
        entity.setCompatibilityNotes(notes);
        entity.setPurpose(spec.getTargetProduct());
        entity.setAssembled(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setAssembledAt(LocalDateTime.now());
        repository.save(entity);

        com.industry.simulator.common.model.Component payload =
                com.industry.simulator.common.model.Component.builder()
                        .id(batchId)
                        .name(spec.getOutputMaterial())
                        .type(spec.getOutputType())
                        .quantity(1.0)
                        .unit("unit")
                        .batchId(batchId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .producer(com.industry.simulator.common.model.Component.Producer.builder()
                                .service(SERVICE)
                                .factory(spec.getFactory())
                                .build())
                        .purpose(com.industry.simulator.common.model.Component.Purpose.builder()
                                .targetProduct(spec.getTargetProduct())
                                .targetComponent(spec.getTargetComponent())
                                .description(spec.getDescription())
                                .build())
                        .compatibleForAssembly(compatible)
                        .components(consumed.stream()
                                .map(ProcessingCompletedEvent::getPayload)
                                .collect(Collectors.toList()))
                        .build();

        return ComponentAssembledEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("COMPONENT_CREATED")
                .timestamp(System.currentTimeMillis() / 1000)
                .payload(payload)
                .build();
    }

    private void handleError(Exception ex, List<ProcessingCompletedEvent> consumed) {
        for (ProcessingCompletedEvent event : consumed) {
            try {
                Component errorComponent = new Component();
                errorComponent.setBatchId(UUID.randomUUID().toString());
                errorComponent.setSourceBatchIds(event.getPayload().getBatchId());
                errorComponent.setComponentName(event.getPayload().getName());
                errorComponent.setComponentType(event.getPayload().getType());
                errorComponent.setQuantity(1.0);
                errorComponent.setUnit("unit");
                errorComponent.setBomValidated(false);
                errorComponent.setCompatible(false);
                errorComponent.setCompatibilityNotes("Falha: " + ex.getMessage());
                errorComponent.setAssembled(false);
                errorComponent.setCreatedAt(LocalDateTime.now());
                repository.save(errorComponent);
            } catch (Exception persistError) {
                log.error("{} | {} | Erro ao gravar falha", event.getEventId(), SERVICE, persistError);
            }
        }
    }
}
