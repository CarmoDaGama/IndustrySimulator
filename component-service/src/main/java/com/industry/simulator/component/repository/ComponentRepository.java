package com.industry.simulator.component.repository;

import com.industry.simulator.component.entity.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComponentRepository extends JpaRepository<Component, Long> {
    List<Component> findByBatchId(String batchId);
    List<Component> findByComponentType(String componentType);
    List<Component> findByAssembled(boolean assembled);
    List<Component> findByBomValidated(boolean bomValidated);
}
