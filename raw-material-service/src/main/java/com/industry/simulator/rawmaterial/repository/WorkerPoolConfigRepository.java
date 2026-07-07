package com.industry.simulator.rawmaterial.repository;

import com.industry.simulator.rawmaterial.entity.WorkerPoolConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerPoolConfigRepository extends JpaRepository<WorkerPoolConfig, Long> {
}
