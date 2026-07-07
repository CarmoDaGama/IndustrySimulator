package com.industry.simulator.assembly.repository;

import com.industry.simulator.assembly.entity.PipelineStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PipelineStepRepository extends JpaRepository<PipelineStep, Long> {
    List<PipelineStep> findAllByIsActiveOrderByStepOrderAsc(boolean isActive);
}
