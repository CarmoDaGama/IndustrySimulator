package com.industry.simulator.processing.controller;

import com.industry.simulator.processing.service.ProcessingWorkerPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Portal de configurações: número de Workers (Threads) activos nesta
 * camada de produção.
 */
@RestController
@RequestMapping("/api/processing/workers")
@CrossOrigin("*")
public class WorkerPoolController {

    @Autowired
    private ProcessingWorkerPoolService workerPoolService;

    @GetMapping
    public ResponseEntity<Map<String, Integer>> status() {
        return ResponseEntity.ok(Map.of(
                "workerCount", workerPoolService.getWorkerCount(),
                "queueSize", workerPoolService.getQueueSize()
        ));
    }

    @PutMapping
    public ResponseEntity<Map<String, Integer>> resize(@RequestBody Map<String, Integer> body) {
        int workerCount = body.getOrDefault("workerCount", 1);
        int applied = workerPoolService.resize(workerCount);
        return ResponseEntity.ok(Map.of("workerCount", applied));
    }
}
