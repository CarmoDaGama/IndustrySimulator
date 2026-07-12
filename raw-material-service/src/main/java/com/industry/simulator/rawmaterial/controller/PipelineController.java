package com.industry.simulator.rawmaterial.controller;

import com.industry.simulator.rawmaterial.entity.PipelineStep;
import com.industry.simulator.rawmaterial.repository.PipelineStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pipeline da Camada 1 (Extracção). Quando existirem etapas configuradas
 * aqui, são elas que definem as fases e os tempos da extracção. Sem etapas,
 * usam-se os tempos por recurso definidos na configuração de extracção.
 */
@RestController
@RequestMapping("/api/raw-materials/pipeline")
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
