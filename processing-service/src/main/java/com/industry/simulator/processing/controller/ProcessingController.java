package com.industry.simulator.processing.controller;

import com.industry.simulator.processing.dto.ProcessingResponse;
import com.industry.simulator.processing.service.ProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/processing")
public class ProcessingController {

    @Autowired
    private ProcessingService service;

    @GetMapping
    public ResponseEntity<List<ProcessingResponse>> getAllProcessed() {
        return ResponseEntity.ok(service.getAllProcessed());
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<ProcessingResponse>> getProcessedByBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(service.getProcessedByBatch(batchId));
    }

    @GetMapping("/type/{materialType}")
    public ResponseEntity<List<ProcessingResponse>> getProcessedByType(@PathVariable String materialType) {
        return ResponseEntity.ok(service.getProcessedByType(materialType));
    }

    @GetMapping("/successful")
    public ResponseEntity<List<ProcessingResponse>> getSuccessfulProcessing() {
        return ResponseEntity.ok(service.getSuccessfulProcessing());
    }

    @GetMapping("/failed")
    public ResponseEntity<List<ProcessingResponse>> getFailedProcessing() {
        return ResponseEntity.ok(service.getFailedProcessing());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Processing Service is running");
    }
}
