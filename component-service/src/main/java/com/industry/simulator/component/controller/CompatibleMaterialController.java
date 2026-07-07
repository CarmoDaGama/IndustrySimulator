package com.industry.simulator.component.controller;

import com.industry.simulator.component.entity.CompatibleMaterial;
import com.industry.simulator.component.repository.CompatibleMaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/components/bom/compatible-materials")
@CrossOrigin("*")
public class CompatibleMaterialController {

    @Autowired
    private CompatibleMaterialRepository repository;

    @GetMapping
    public ResponseEntity<List<CompatibleMaterial>> list() {
        return ResponseEntity.ok(repository.findAll());
    }

    @PostMapping
    public ResponseEntity<CompatibleMaterial> add(@RequestBody CompatibleMaterial material) {
        material.setId(null);
        return ResponseEntity.ok(repository.save(material));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
