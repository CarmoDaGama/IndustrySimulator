package com.industry.simulator.assembly.controller;

import com.industry.simulator.assembly.dto.MarketOrderResponse;
import com.industry.simulator.assembly.service.MarketOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Apenas leitura/monitorização. As encomendas são geradas automaticamente
 * pelos clientes fictícios da Camada 6 ({@code CustomerSimulatorService}) —
 * não existe forma de criar uma encomenda por REST, o que mantém o HTTP
 * restrito a monitorização, logs e configurações.
 */
@RestController
@RequestMapping("/api/market-orders")
@CrossOrigin("*")
public class MarketOrderController {

    @Autowired
    private MarketOrderService service;

    @GetMapping
    public ResponseEntity<List<MarketOrderResponse>> getAllOrders() {
        return ResponseEntity.ok(service.getAllOrders());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MarketOrderResponse>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.getOrdersByStatus(status));
    }

    @GetMapping("/product/{productName}")
    public ResponseEntity<List<MarketOrderResponse>> getOrdersByProduct(@PathVariable String productName) {
        return ResponseEntity.ok(service.getOrdersByProduct(productName));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<MarketOrderResponse> getOrderById(@PathVariable String orderId) {
        return service.getOrderById(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Market Order Service is running");
    }
}
