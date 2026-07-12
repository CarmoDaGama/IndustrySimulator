package com.industry.simulator.assembly.repository;

import com.industry.simulator.assembly.entity.CustomerSimulatorConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerSimulatorConfigRepository extends JpaRepository<CustomerSimulatorConfig, Long> {
}
