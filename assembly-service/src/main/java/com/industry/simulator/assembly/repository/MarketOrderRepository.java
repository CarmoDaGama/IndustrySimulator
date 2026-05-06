package com.industry.simulator.assembly.repository;

import com.industry.simulator.assembly.entity.MarketOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarketOrderRepository extends JpaRepository<MarketOrder, Long> {
    Optional<MarketOrder> findByOrderId(String orderId);
    List<MarketOrder> findByProductName(String productName);
    List<MarketOrder> findByStatus(String status);
}
