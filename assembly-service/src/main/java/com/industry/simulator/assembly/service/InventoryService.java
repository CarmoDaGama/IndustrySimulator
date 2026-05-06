package com.industry.simulator.assembly.service;

import com.industry.simulator.assembly.dto.InventoryResponse;
import com.industry.simulator.assembly.entity.Inventory;
import com.industry.simulator.assembly.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    public InventoryResponse addInventory(String productId, String productName, int quantity) {
        log.info("Adding inventory for product {} with quantity {}", productName, quantity);

        List<Inventory> existingList = inventoryRepository.findByProductName(productName);
        Inventory inventory = existingList.isEmpty() 
                ? Inventory.builder()
                        .productId(productId)
                        .productName(productName)
                        .quantity(0)
                        .reservedQuantity(0)
                        .location("Warehouse")
                        .build()
                : existingList.get(0);

        inventory.setQuantity(inventory.getQuantity() + quantity);
        inventory.setAvailableQuantity(inventory.getQuantity() - inventory.getReservedQuantity());
        inventory.setLastUpdated(LocalDateTime.now());

        inventoryRepository.save(inventory);
        return toResponse(inventory);
    }

    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<InventoryResponse> getInventoryByProduct(String productName) {
        return inventoryRepository.findByProductName(productName)
                .stream()
                .map(this::toResponse)
                .findFirst();
    }

    public List<InventoryResponse> getLowStockItems(int threshold) {
        return inventoryRepository.findAll().stream()
                .filter(inv -> inv.getAvailableQuantity() <= threshold)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())
                .location(inventory.getLocation())
                .lastUpdated(inventory.getLastUpdated())
                .build();
    }
}
