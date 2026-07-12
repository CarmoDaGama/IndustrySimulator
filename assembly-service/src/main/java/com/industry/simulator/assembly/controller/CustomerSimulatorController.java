package com.industry.simulator.assembly.controller;

import com.industry.simulator.assembly.entity.CustomerSimulatorConfig;
import com.industry.simulator.assembly.service.AssemblyWorkerPoolService;
import com.industry.simulator.assembly.service.CustomerSimulatorService;
import com.industry.simulator.common.worker.WorkerActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Portal de Configurações — simulação de clientes fictícios (Camada 6).
 */
@RestController
@RequestMapping("/api/market/customers")
@CrossOrigin("*")
public class CustomerSimulatorController {

    @Autowired
    private CustomerSimulatorService simulator;

    @Autowired
    private AssemblyWorkerPoolService assemblyWorkerPoolService;

    @GetMapping
    public ResponseEntity<CustomerSimulatorConfig> config() {
        return ResponseEntity.ok(simulator.config());
    }

    @PutMapping
    public ResponseEntity<CustomerSimulatorConfig> update(@RequestBody CustomerSimulatorConfig changes) {
        return ResponseEntity.ok(simulator.update(changes));
    }

    /** O que cada cliente simulado está a fazer neste momento. */
    @GetMapping("/activity")
    public ResponseEntity<List<WorkerActivity>> activity() {
        return ResponseEntity.ok(simulator.getActivities());
    }

    /** Produtos que a fábrica sabe montar (vêm das regras da Camada 4). */
    @GetMapping("/catalog")
    public ResponseEntity<List<String>> catalog() {
        return ResponseEntity.ok(assemblyWorkerPoolService.getProducibleProducts());
    }
}
