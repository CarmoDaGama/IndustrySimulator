package com.industry.simulator.component.repository;

import com.industry.simulator.component.entity.CompatibleMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompatibleMaterialRepository extends JpaRepository<CompatibleMaterial, Long> {
    Optional<CompatibleMaterial> findByMaterialTypeIgnoreCase(String materialType);
}
