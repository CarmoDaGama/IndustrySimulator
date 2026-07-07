package com.industry.simulator.assembly.service;

import com.industry.simulator.assembly.dto.MarketOrderRequest;
import com.industry.simulator.assembly.dto.MarketOrderResponse;
import com.industry.simulator.assembly.entity.MarketOrder;
import com.industry.simulator.assembly.entity.Inventory;
import com.industry.simulator.assembly.repository.MarketOrderRepository;
import com.industry.simulator.assembly.repository.InventoryRepository;
import com.industry.simulator.assembly.websocket.OrderStatusPublisher;
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

    @Autowired
    private OrderStatusPublisher orderStatusPublisher;

    public MarketOrderResponse createOrder(MarketOrderRequest request) {
        log.info("Creating market order for product {} with quantity {}", request.getProductType(), request.getQuantity());

        // Check inventory availability
        List<Inventory> inventoryList = inventoryRepository.findByProductName(request.getProductType());
        Inventory inventory = inventoryList.isEmpty() ? null : inventoryList.get(0);

        MarketOrder order = MarketOrder.builder()
                .orderId(UUID.randomUUID().toString())
                .productType(request.getProductType())
                .bomVersion(request.getBomVersion())
                .quantity(request.getQuantity())
                .customerName(request.getCustomerName())
                .priority(request.getPriority())
                .requiredDeliveryDate(request.getRequiredDeliveryDate())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        if (inventory != null && inventory.getAvailableQuantity() >= request.getQuantity()) {
            // Allocate inventory
            inventory.setReservedQuantity(inventory.getReservedQuantity() + request.getQuantity());
            inventory.setAvailableQuantity(inventory.getQuantity() - inventory.getReservedQuantity());
            inventoryRepository.save(inventory);

            order.setStatus("ALLOCATED");
            order.setFulfilledAt(LocalDateTime.now());
            log.info("Order allocated: {}", order.getOrderId());
        } else {
            // Fica PENDENTE até a Camada 5 (Inventário) notificar reabastecimento
            // - ver InventoryUpdatedConsumer / tryAllocatePendingOrders.
            order.setStatus("PENDING");
            log.warn("Order pending - insufficient inventory: {}", order.getOrderId());
        }

        marketOrderRepository.save(order);

        // Publish WebSocket event
        orderStatusPublisher.publishOrderCreated(order.getOrderId(), request.getProductType(),
                                                 request.getQuantity(), request.getCustomerName());

        return toResponse(order);
    }

    /**
     * Reprocessa os pedidos PENDENTES de um produto assim que a Camada 5
     * (Inventário) notifica reabastecimento (evento inventory-updated).
     * Aloca por ordem de prioridade e depois FIFO, enquanto houver stock
     * disponível.
     */
    public void tryAllocatePendingOrders(String productType) {
        List<MarketOrder> pending = marketOrderRepository
                .findByProductTypeAndStatusOrderByPriorityDescCreatedAtAsc(productType, "PENDING");
        if (pending.isEmpty()) {
            return;
        }

        for (MarketOrder order : pending) {
            List<Inventory> inventoryList = inventoryRepository.findByProductName(productType);
            if (inventoryList.isEmpty()) {
                break;
            }
            Inventory inventory = inventoryList.get(0);
            if (inventory.getAvailableQuantity() < order.getQuantity()) {
                break; // ainda não há stock suficiente para este (nem para os seguintes, FIFO)
            }

            inventory.setReservedQuantity(inventory.getReservedQuantity() + order.getQuantity());
            inventory.setAvailableQuantity(inventory.getQuantity() - inventory.getReservedQuantity());
            inventoryRepository.save(inventory);

            order.setStatus("ALLOCATED");
            order.setFulfilledAt(LocalDateTime.now());
            marketOrderRepository.save(order);

            log.info("Pedido {} desbloqueado (era PENDING) após reposição de stock de {}", order.getOrderId(), productType);
            orderStatusPublisher.publishOrderAssembled(order.getOrderId(), order.getProductType(),
                    order.getQuantity(), order.getCustomerName());
        }
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

    public List<MarketOrderResponse> getOrdersByProduct(String productType) {
        // Updated to use productType column name logic
        return marketOrderRepository.findAll().stream()
                .filter(o -> o.getProductType().equals(productType))
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
                .productName(order.getProductType())
                .quantity(order.getQuantity())
                .status(order.getStatus())
                .errorMessage(order.getErrorMessage())
                .createdAt(order.getCreatedAt())
                .fulfilledAt(order.getFulfilledAt())
                .build();
    }
}
