package com.industry.simulator.component.repository;

import com.industry.simulator.component.entity.WorkerPoolConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerPoolConfigRepository extends JpaRepository<WorkerPoolConfig, Long> {
}
