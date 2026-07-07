package com.industry.simulator.assembly.repository;

import com.industry.simulator.assembly.entity.WorkerPoolConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerPoolConfigRepository extends JpaRepository<WorkerPoolConfig, Long> {
}
