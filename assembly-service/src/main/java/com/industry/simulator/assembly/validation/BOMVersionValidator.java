package com.industry.simulator.assembly.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import java.util.regex.Pattern;

/**
 * Validator for BOM version format: vX.Y.Z (e.g., v1.0.0, v2.1.5)
 */
@Slf4j
public class BOMVersionValidator implements ConstraintValidator<ValidBOMVersion, String> {
    
    private static final Pattern BOM_VERSION_PATTERN = Pattern.compile("^v\\d+\\.\\d+\\.\\d+$");
    
    @Override
    public void initialize(ValidBOMVersion annotation) {
        // Initialization logic if needed
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        
        boolean isValid = BOM_VERSION_PATTERN.matcher(value).matches();
        
        if (!isValid) {
            log.warn("Invalid BOM version format: {}", value);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("BOM version '%s' does not match format vX.Y.Z (e.g., v1.0.0)", value)
            ).addConstraintViolation();
        }
        
        return isValid;
    }
}
