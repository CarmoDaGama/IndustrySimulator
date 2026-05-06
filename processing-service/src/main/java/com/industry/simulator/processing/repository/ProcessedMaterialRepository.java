package com.industry.simulator.processing.repository;

import com.industry.simulator.processing.entity.ProcessedMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessedMaterialRepository extends JpaRepository<ProcessedMaterial, Long> {
    List<ProcessedMaterial> findByBatchId(String batchId);
    List<ProcessedMaterial> findByMaterialType(String materialType);
    List<ProcessedMaterial> findBySuccess(boolean success);
}
