package com.industry.simulator.assembly.service;

import com.industry.simulator.assembly.dto.MarketOrderResponse;
import com.industry.simulator.assembly.entity.MarketOrder;
import com.industry.simulator.assembly.entity.Inventory;
import com.industry.simulator.assembly.repository.MarketOrderRepository;
import com.industry.simulator.assembly.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MarketOrderService {

    @Autowired
    private MarketOrderRepository marketOrderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    public MarketOrderResponse createOrder(String productName, int quantity) {
        log.info("Creating market order for product {} with quantity {}", productName, quantity);

        // Check inventory availability
        List<Inventory> inventoryList = inventoryRepository.findByProductName(productName);
        Inventory inventory = inventoryList.isEmpty() ? null : inventoryList.get(0);

        MarketOrder order = MarketOrder.builder()
                .orderId(UUID.randomUUID().toString())
                .productName(productName)
                .quantity(quantity)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        if (inventory != null && inventory.getAvailableQuantity() >= quantity) {
            // Allocate inventory
            inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
            inventory.setAvailableQuantity(inventory.getQuantity() - inventory.getReservedQuantity());
            inventoryRepository.save(inventory);

            order.setStatus("ALLOCATED");
            order.setFulfilledAt(LocalDateTime.now());
            log.info("Order allocated: {}", order.getOrderId());
        } else {
            order.setStatus("FAILED");
            order.setErrorMessage("Insufficient inventory");
            log.warn("Order failed - insufficient inventory: {}", order.getOrderId());
        }

        marketOrderRepository.save(order);
        return toResponse(order);
    }

    public List<MarketOrderResponse> getAllOrders() {
        return marketOrderRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MarketOrderResponse> getOrdersByStatus(String status) {
        return marketOrderRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<MarketOrderResponse> getOrdersByProduct(String productName) {
        return marketOrderRepository.findByProductName(productName).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<MarketOrderResponse> getOrderById(String orderId) {
        return marketOrderRepository.findByOrderId(orderId)
                .map(this::toResponse);
    }

    private MarketOrderResponse toResponse(MarketOrder order) {
        return MarketOrderResponse.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .status(order.getStatus())
                .errorMessage(order.getErrorMessage())
                .createdAt(order.getCreatedAt())
                .fulfilledAt(order.getFulfilledAt())
                .build();
    }
}
