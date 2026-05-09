package com.industry.simulator.rawmaterial.service;

import com.industry.simulator.common.events.RawMaterialProducedEvent;
import com.industry.simulator.common.model.Component;
import com.industry.simulator.rawmaterial.entity.RawMaterial;
import com.industry.simulator.rawmaterial.kafka.RawMaterialProducer;
import com.industry.simulator.rawmaterial.repository.RawMaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class RawMaterialService {

    private static final Logger log = LoggerFactory.getLogger(RawMaterialService.class);

    @Autowired
    private RawMaterialRepository repository;

    @Autowired
    private RawMaterialProducer producer;

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

        // Simulate extraction delay asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                long extractionDuration = 10000;
                long transportDuration = 5000;
                
                log.info("{} | raw-material-service | Starting EXTRACTION ({}ms)", batchId, extractionDuration);
                Thread.sleep(extractionDuration);
                
                log.info("{} | raw-material-service | Starting TRANSPORT ({}ms)", batchId, transportDuration);
                Thread.sleep(transportDuration);

                // Build v2 compliant component
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
                                .factory("mining-site-alpha")
                                .build())
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
