package com.industry.simulator.rawmaterial.repository;

import com.industry.simulator.rawmaterial.entity.ExtractionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExtractionConfigRepository extends JpaRepository<ExtractionConfig, Long> {
    List<ExtractionConfig> findByActiveTrue();
    Optional<ExtractionConfig> findFirstByMaterialNameIgnoreCaseAndActiveTrue(String materialName);
}
