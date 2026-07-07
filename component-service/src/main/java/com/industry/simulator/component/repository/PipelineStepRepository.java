package com.industry.simulator.component.repository;

import com.industry.simulator.component.entity.PipelineStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PipelineStepRepository extends JpaRepository<PipelineStep, Long> {
    List<PipelineStep> findAllByIsActiveOrderByStepOrderAsc(boolean isActive);
}
