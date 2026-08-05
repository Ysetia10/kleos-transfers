package com.kleos.transfers.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates ISO 3166-1 alpha-3 country codes.
 */
@Documented
@Constraint(validatedBy = IsoCountryCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface IsoCountryCode {

    String message() default "must be a valid ISO 3166-1 alpha-3 country code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
