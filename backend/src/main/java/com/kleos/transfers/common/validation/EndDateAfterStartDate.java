package com.kleos.transfers.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a request's {@code endDate} is after {@code startDate}.
 *
 * <p>Set {@link #inclusive()} for ranges that may start and end on the same day,
 * such as a one-day injury spell.
 */
@Documented
@Constraint(validatedBy = EndDateAfterStartDateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EndDateAfterStartDate {

    String message() default "endDate must be after startDate";

    boolean inclusive() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
