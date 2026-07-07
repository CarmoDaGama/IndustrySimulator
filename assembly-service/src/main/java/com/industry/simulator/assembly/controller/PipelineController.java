package com.industry.simulator.assembly.controller;

import com.industry.simulator.assembly.entity.PipelineStep;
import com.industry.simulator.assembly.repository.PipelineStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assembly/pipeline")
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
        repository.deleteAll();
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
