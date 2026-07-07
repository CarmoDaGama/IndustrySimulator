package com.industry.simulator.processing.repository;

import com.industry.simulator.processing.entity.WorkerPoolConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerPoolConfigRepository extends JpaRepository<WorkerPoolConfig, Long> {
}
