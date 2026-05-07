package com.industry.simulator.assembly.repository;

import com.industry.simulator.assembly.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductId(String productId);
    List<Product> findByBatchId(String batchId);
    List<Product> findByAssembled(boolean assembled);
    List<Product> findByStatus(String status);
}
