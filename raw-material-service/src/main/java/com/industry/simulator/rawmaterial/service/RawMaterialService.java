package com.industry.simulator.rawmaterial.service;

import com.industry.simulator.common.events.RawMaterialProducedEvent;
import com.industry.simulator.common.model.Component;
import com.industry.simulator.rawmaterial.entity.ExtractionConfig;
import com.industry.simulator.rawmaterial.entity.RawMaterial;
import com.industry.simulator.rawmaterial.kafka.RawMaterialProducer;
import com.industry.simulator.rawmaterial.repository.ExtractionConfigRepository;
import com.industry.simulator.rawmaterial.repository.RawMaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A produção autónoma e contínua da Camada 1 é feita por
 * {@link RawMaterialWorkerPoolService}. Este serviço expõe apenas uma via
 * manual (REST) para gestão/testes, reaproveitando a mesma configuração
 * ({@link ExtractionConfig}) em vez de tempos fixos no código.
 */
@Service
public class RawMaterialService {

    private static final Logger log = LoggerFactory.getLogger(RawMaterialService.class);
    private static final long FALLBACK_EXTRACTION_MS = 3000;
    private static final long FALLBACK_TRANSPORT_MS = 1500;

    @Autowired
    private RawMaterialRepository repository;

    @Autowired
    private RawMaterialProducer producer;

    @Autowired
    private ExtractionConfigRepository extractionConfigRepository;

    public RawMaterial createRawMaterial(String materialName, String materialType, double quantity, String unit) {
        String batchId = UUID.randomUUID().toString();

        RawMaterial material = new RawMaterial();
        material.setMaterialName(materialName);
        material.setMaterialType(materialType);
        material.setQuantity(quantity);
        material.setUnit(unit);
        material.setBatchId(batchId);
        material.setCreatedAt(LocalDateTime.now());
        material.setUpdatedAt(LocalDateTime.now());

        RawMaterial saved = repository.save(material);
        log.info("{} | raw-material-service | Registered extraction request for {}", batchId, materialName);

        Optional<ExtractionConfig> matchingConfig =
                extractionConfigRepository.findFirstByMaterialNameIgnoreCaseAndActiveTrue(materialName);
        long extractionDuration = matchingConfig.map(ExtractionConfig::getExtractionDurationMs).orElse(FALLBACK_EXTRACTION_MS);
        long transportDuration = matchingConfig.map(ExtractionConfig::getTransportDurationMs).orElse(FALLBACK_TRANSPORT_MS);
        String factory = matchingConfig.map(ExtractionConfig::getFactory).orElse("mining-site-alpha");
        Component.Purpose purpose = matchingConfig
                .map(c -> Component.Purpose.builder()
                        .targetProduct(c.getTargetProduct())
                        .targetComponent(c.getTargetComponent())
                        .description(c.getDescription())
                        .build())
                .orElse(null);

        CompletableFuture.runAsync(() -> {
            try {
                log.info("{} | raw-material-service | Starting EXTRACTION ({}ms)", batchId, extractionDuration);
                Thread.sleep(extractionDuration);

                log.info("{} | raw-material-service | Starting TRANSPORT ({}ms)", batchId, transportDuration);
                Thread.sleep(transportDuration);

                Component component = Component.builder()
                        .id(saved.getId())
                        .name(materialName)
                        .type("RAW_MATERIAL")
                        .quantity(quantity)
                        .unit(unit)
                        .batchId(batchId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .producer(Component.Producer.builder()
                                .service("raw-material-service")
                                .factory(factory)
                                .build())
                        .purpose(purpose)
                        .build();

                RawMaterialProducedEvent event = RawMaterialProducedEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("RAW_MATERIAL_EXTRACTED")
                        .batchId(batchId)
                        .material(component)
                        .quantity(quantity)
                        .unit(unit)
                        .timestamp(LocalDateTime.now())
                        .purpose("processing")
                        .sourceService("raw-material-service")
                        .build();

                producer.publishRawMaterialProduced(event);
                log.info("{} | raw-material-service | Extraction and Transport completed. Event published.", batchId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("{} | raw-material-service | Production pipeline interrupted", batchId, e);
            }
        });

        return saved;
    }

    public List<RawMaterial> getAllRawMaterials() {
        return repository.findAll();
    }

    public List<RawMaterial> getRawMaterialsByBatch(String batchId) {
        return repository.findByBatchId(batchId);
    }

    public List<RawMaterial> getRawMaterialsByType(String materialType) {
        return repository.findByMaterialType(materialType);
    }
}
