package com.industry.simulator.rawmaterial.controller;

import com.industry.simulator.rawmaterial.dto.CreateRawMaterialRequest;
import com.industry.simulator.rawmaterial.dto.RawMaterialResponse;
import com.industry.simulator.rawmaterial.entity.RawMaterial;
import com.industry.simulator.rawmaterial.service.RawMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/raw-materials")
public class RawMaterialController {

    @Autowired
    private RawMaterialService service;

    @PostMapping
    public ResponseEntity<RawMaterialResponse> createRawMaterial(@RequestBody CreateRawMaterialRequest request) {
        RawMaterial material = service.createRawMaterial(
                request.getMaterialName(),
                request.getMaterialType(),
                request.getQuantity(),
                request.getUnit()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(material));
    }

    @GetMapping
    public ResponseEntity<List<RawMaterialResponse>> getAllRawMaterials() {
        List<RawMaterial> materials = service.getAllRawMaterials();
        return ResponseEntity.ok(materials.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<List<RawMaterialResponse>> getRawMaterialsByBatch(@PathVariable String batchId) {
        List<RawMaterial> materials = service.getRawMaterialsByBatch(batchId);
        return ResponseEntity.ok(materials.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/type/{materialType}")
    public ResponseEntity<List<RawMaterialResponse>> getRawMaterialsByType(@PathVariable String materialType) {
        List<RawMaterial> materials = service.getRawMaterialsByType(materialType);
        return ResponseEntity.ok(materials.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Raw Material Service is running");
    }

    private RawMaterialResponse toResponse(RawMaterial material) {
        return RawMaterialResponse.builder()
                .id(material.getId())
                .materialName(material.getMaterialName())
                .materialType(material.getMaterialType())
                .quantity(material.getQuantity())
                .unit(material.getUnit())
                .batchId(material.getBatchId())
                .createdAt(material.getCreatedAt())
                .build();
    }
}
