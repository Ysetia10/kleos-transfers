package com.kleos.transfers.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ensures a transfer request has at least one club and that from/to clubs differ when both are set.
 */
@Documented
@Constraint(validatedBy = DistinctClubsValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistinctClubs {

    String message() default "fromClubId and toClubId must differ, and at least one club is required";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
