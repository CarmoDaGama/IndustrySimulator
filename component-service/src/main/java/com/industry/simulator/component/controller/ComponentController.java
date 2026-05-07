package com.industry.simulator.component.controller;

import com.industry.simulator.component.dto.ComponentResponse;
import com.industry.simulator.component.service.ComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/components")
public class ComponentController {

    @Autowired
    private ComponentService service;

    @GetMapping
    public ResponseEntity<List<ComponentResponse>> getAllComponents() {
        return ResponseEntity.ok(service.getAllComponents());
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<ComponentResponse>> getComponentsByBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(service.getComponentsByBatch(batchId));
    }

    @GetMapping("/type/{componentType}")
    public ResponseEntity<List<ComponentResponse>> getComponentsByType(@PathVariable String componentType) {
        return ResponseEntity.ok(service.getComponentsByType(componentType));
    }

    @GetMapping("/assembled")
    public ResponseEntity<List<ComponentResponse>> getAssembledComponents() {
        return ResponseEntity.ok(service.getAssembledComponents());
    }

    @GetMapping("/awaiting-assembly")
    public ResponseEntity<List<ComponentResponse>> getComponentsAwaitingAssembly() {
        return ResponseEntity.ok(service.getComponentsAwaitingAssembly());
    }

    @GetMapping("/compatible")
    public ResponseEntity<List<ComponentResponse>> getCompatibleComponents() {
        return ResponseEntity.ok(service.getCompatibleComponents());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Component Service is running");
    }
}
