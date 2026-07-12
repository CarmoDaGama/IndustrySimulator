package com.industry.simulator.component.repository;

import com.industry.simulator.component.entity.ProductionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionRuleRepository extends JpaRepository<ProductionRule, Long> {
    List<ProductionRule> findByActiveTrue();
}
