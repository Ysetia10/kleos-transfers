package com.kleos.transfers.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a request's {@code endDate} is strictly after {@code startDate}.
 */
@Documented
@Constraint(validatedBy = EndDateAfterStartDateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EndDateAfterStartDate {

    String message() default "endDate must be after startDate";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
