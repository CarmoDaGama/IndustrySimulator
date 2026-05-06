package com.industry.simulator.processing.service;

import com.industry.simulator.processing.dto.ProcessingResponse;
import com.industry.simulator.processing.entity.ProcessedMaterial;
import com.industry.simulator.processing.repository.ProcessedMaterialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProcessingService {

    @Autowired
    private ProcessedMaterialRepository repository;

    public List<ProcessingResponse> getAllProcessed() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProcessingResponse> getProcessedByBatch(String batchId) {
        return repository.findByBatchId(batchId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProcessingResponse> getProcessedByType(String materialType) {
        return repository.findByMaterialType(materialType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProcessingResponse> getSuccessfulProcessing() {
        return repository.findBySuccess(true).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProcessingResponse> getFailedProcessing() {
        return repository.findBySuccess(false).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ProcessingResponse toResponse(ProcessedMaterial material) {
        return ProcessingResponse.builder()
                .id(material.getId())
                .batchId(material.getBatchId())
                .materialName(material.getMaterialName())
                .materialType(material.getMaterialType())
                .quantity(material.getQuantity())
                .unit(material.getUnit())
                .processingType(material.getProcessingType())
                .processingDurationMs(material.getProcessingDurationMs())
                .purpose(material.getPurpose())
                .success(material.isSuccess())
                .errorMessage(material.getErrorMessage())
                .createdAt(material.getCreatedAt())
                .completedAt(material.getCompletedAt())
                .build();
    }
}
