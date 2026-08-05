package com.kleos.transfers.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates FIFA association nationality codes (for example ENG, GER, NED).
 */
@Documented
@Constraint(validatedBy = FootballNationalityCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface FootballNationalityCode {

    String message() default "must be a valid FIFA nationality code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
