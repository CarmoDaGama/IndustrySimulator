package com.industry.simulator.assembly.controller;

import com.industry.simulator.assembly.service.AssemblyWorkerPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assembly/workers")
@CrossOrigin("*")
public class WorkerPoolController {

    @Autowired
    private AssemblyWorkerPoolService workerPoolService;

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
