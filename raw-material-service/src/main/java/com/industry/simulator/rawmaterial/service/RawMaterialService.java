package com.industry.simulator.rawmaterial.service;

import com.industry.simulator.rawmaterial.entity.RawMaterial;
import com.industry.simulator.rawmaterial.repository.RawMaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Consultas sobre a matéria-prima extraída. A produção em si é feita
 * autonomamente pelos Workers ({@link RawMaterialWorkerPoolService}) — não
 * existe forma de disparar extracção por REST, como o enunciado exige.
 */
@Service
public class RawMaterialService {

    @Autowired
    private RawMaterialRepository repository;

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
