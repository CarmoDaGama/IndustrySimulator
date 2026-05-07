package com.industry.simulator.assembly.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom validation annotation for BOM version format (vX.Y.Z)
 */
@Documented
@Constraint(validatedBy = BOMVersionValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBOMVersion {
    String message() default "Invalid BOM version format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
