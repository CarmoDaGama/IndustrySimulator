package com.industry.simulator.component.controller;

import com.industry.simulator.component.service.ComponentWorkerPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.industry.simulator.common.worker.WorkerActivity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/components/workers")
@CrossOrigin("*")
public class WorkerPoolController {

    @Autowired
    private ComponentWorkerPoolService workerPoolService;

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

    /** Estado ao vivo de cada Worker: etapa em execução, progresso e lote. */
    @GetMapping("/activity")
    public ResponseEntity<List<WorkerActivity>> activity() {
        return ResponseEntity.ok(workerPoolService.getActivities());
    }
}
