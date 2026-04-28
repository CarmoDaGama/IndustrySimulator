package com.industry.simulator.rawmaterial.service;

import com.industry.simulator.common.events.RawMaterialProducedEvent;
import com.industry.simulator.common.model.Component;
import com.industry.simulator.rawmaterial.entity.RawMaterial;
import com.industry.simulator.rawmaterial.kafka.RawMaterialProducer;
import com.industry.simulator.rawmaterial.repository.RawMaterialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class RawMaterialService {

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
        log.info("Created raw material: {} with batch: {}", materialName, batchId);

        // Publish event to Kafka
        Component component = new Component();
        component.setId(saved.getId());
        component.setName(materialName);
        component.setType("RAW_MATERIAL");
        component.setQuantity(quantity);
        component.setUnit(unit);
        component.setBatchId(batchId);
        component.setSourceService("raw-material-service");

        RawMaterialProducedEvent event = RawMaterialProducedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .batchId(batchId)
                .material(component)
                .quantity(quantity)
                .unit(unit)
                .timestamp(LocalDateTime.now())
                .purpose("processing")
                .sourceService("raw-material-service")
                .build();

        producer.publishRawMaterialProduced(event);

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
