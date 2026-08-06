package com.kleos.transfers.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Accepts European season labels ({@code 2024/25}) or calendar-year labels ({@code 2024}).
 */
@Documented
@Constraint(validatedBy = SeasonLabelValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SeasonLabel {

    String message() default "must be a season label like 2024/25 or 2024";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
