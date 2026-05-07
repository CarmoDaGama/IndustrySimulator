package com.industry.simulator.component.service;

import com.industry.simulator.component.dto.ComponentResponse;
import com.industry.simulator.component.entity.Component;
import com.industry.simulator.component.repository.ComponentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ComponentService {

    @Autowired
    private ComponentRepository repository;

    public List<ComponentResponse> getAllComponents() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ComponentResponse> getComponentsByBatch(String batchId) {
        return repository.findByBatchId(batchId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ComponentResponse> getComponentsByType(String componentType) {
        return repository.findByComponentType(componentType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ComponentResponse> getAssembledComponents() {
        return repository.findByAssembled(true).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ComponentResponse> getComponentsAwaitingAssembly() {
        return repository.findByAssembled(false).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ComponentResponse> getCompatibleComponents() {
        return repository.findAll().stream()
                .filter(Component::isCompatible)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ComponentResponse toResponse(Component component) {
        return ComponentResponse.builder()
                .id(component.getId())
                .batchId(component.getBatchId())
                .componentName(component.getComponentName())
                .componentType(component.getComponentType())
                .quantity(component.getQuantity())
                .unit(component.getUnit())
                .processingType(component.getProcessingType())
                .bomValidated(component.isBomValidated())
                .compatible(component.isCompatible())
                .compatibilityNotes(component.getCompatibilityNotes())
                .purpose(component.getPurpose())
                .assembled(component.isAssembled())
                .createdAt(component.getCreatedAt())
                .assembledAt(component.getAssembledAt())
                .build();
    }
}
