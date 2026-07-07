package com.industry.simulator.rawmaterial.controller;

import com.industry.simulator.rawmaterial.entity.ExtractionConfig;
import com.industry.simulator.rawmaterial.repository.ExtractionConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Portal de configurações: recursos que a Camada 1 extrai autonomamente.
 */
@RestController
@RequestMapping("/api/raw-materials/extraction-config")
@CrossOrigin("*")
public class ExtractionConfigController {

    @Autowired
    private ExtractionConfigRepository repository;

    @GetMapping
    public ResponseEntity<List<ExtractionConfig>> list() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<ExtractionConfig> create(@RequestBody ExtractionConfig config) {
        config.setId(null);
        return ResponseEntity.ok(repository.save(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExtractionConfig> update(@PathVariable Long id, @RequestBody ExtractionConfig config) {
        config.setId(id);
        return ResponseEntity.ok(repository.save(config));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
