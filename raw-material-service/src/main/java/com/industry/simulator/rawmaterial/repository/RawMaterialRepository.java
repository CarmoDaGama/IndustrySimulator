package com.industry.simulator.rawmaterial.repository;

import com.industry.simulator.rawmaterial.entity.RawMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawMaterialRepository extends JpaRepository<RawMaterial, String> {
    List<RawMaterial> findByBatchId(String batchId);
    List<RawMaterial> findByMaterialType(String materialType);
}
