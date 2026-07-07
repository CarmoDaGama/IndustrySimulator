package com.industry.simulator.component.service;

import com.industry.simulator.component.entity.CompatibleMaterial;
import com.industry.simulator.component.repository.CompatibleMaterialRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Validação de compatibilidade BOM orientada a configuração em base de
 * dados (nada de listas fixas no código - editável via portal de
 * configurações em /api/components/bom/compatible-materials).
 */
@Service
public class BOMValidationService {

    private static final Logger log = LoggerFactory.getLogger(BOMValidationService.class);
    private static final List<String> DEFAULT_MATERIALS = List.of(
            "steel", "aluminum", "plastic", "rubber", "copper", "wood"
    );

    @Autowired
    private CompatibleMaterialRepository repository;

    @PostConstruct
    public void seedDefaults() {
        if (repository.count() == 0) {
            DEFAULT_MATERIALS.forEach(m -> repository.save(new CompatibleMaterial(m)));
        }
    }

    public boolean validateComponentCompatibility(String materialType) {
        boolean isCompatible = materialType != null
                && repository.findByMaterialTypeIgnoreCase(materialType).isPresent();
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
