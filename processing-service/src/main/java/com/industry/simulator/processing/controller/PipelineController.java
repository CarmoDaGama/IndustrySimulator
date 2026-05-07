package com.industry.simulator.processing.controller;

import com.industry.simulator.processing.entity.PipelineStep;
import com.industry.simulator.processing.repository.PipelineStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/processing/pipeline")
@CrossOrigin("*")
public class PipelineController {

    @Autowired
    private PipelineStepRepository repository;

    @GetMapping
    public ResponseEntity<List<PipelineStep>> getPipeline() {
        return ResponseEntity.ok(repository.findAllByIsActiveOrderByStepOrderAsc(true));
    }

    @PostMapping
    public ResponseEntity<List<PipelineStep>> savePipeline(@RequestBody List<PipelineStep> steps) {
        // Clear existing steps for simplicity in this simulator
        repository.deleteAll();
        
        // Save new steps
        for (int i = 0; i < steps.size(); i++) {
            PipelineStep step = steps.get(i);
            step.setId(null);
            step.setStepOrder(i + 1);
            step.setActive(true);
            repository.save(step);
        }
        
        return ResponseEntity.ok(repository.findAllByIsActiveOrderByStepOrderAsc(true));
    }
}
