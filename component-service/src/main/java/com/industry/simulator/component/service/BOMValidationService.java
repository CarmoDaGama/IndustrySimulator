package com.industry.simulator.component.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class BOMValidationService {

    private static final Set<String> COMPATIBLE_MATERIALS = new HashSet<>();

    static {
        // Define compatible material types for BOM
        COMPATIBLE_MATERIALS.add("steel");
        COMPATIBLE_MATERIALS.add("aluminum");
        COMPATIBLE_MATERIALS.add("plastic");
        COMPATIBLE_MATERIALS.add("rubber");
        COMPATIBLE_MATERIALS.add("copper");
        COMPATIBLE_MATERIALS.add("wood");
    }

    public boolean validateComponentCompatibility(String materialType) {
        boolean isCompatible = COMPATIBLE_MATERIALS.contains(materialType.toLowerCase());
        log.info("BOM validation for material type {}: {}", materialType, isCompatible);
        return isCompatible;
    }

    public boolean validateBOMRequirements(String componentType, double requiredQuantity, double availableQuantity) {
        boolean hasSufficientQuantity = availableQuantity >= requiredQuantity;
        log.info("BOM quantity validation for {}: required={}, available={}, valid={}", 
                componentType, requiredQuantity, availableQuantity, hasSufficientQuantity);
        return hasSufficientQuantity;
    }
}
