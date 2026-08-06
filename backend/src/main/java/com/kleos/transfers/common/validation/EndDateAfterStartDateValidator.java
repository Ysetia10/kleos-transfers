package com.kleos.transfers.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

/**
 * Ensures {@link DateRangeRequest#endDate()} is after {@link DateRangeRequest#startDate()}.
 *
 * <p>A null {@code endDate} is treated as valid so open-ended ranges (ongoing injuries)
 * can use the same contract.
 */
public class EndDateAfterStartDateValidator implements ConstraintValidator<EndDateAfterStartDate, DateRangeRequest> {

    private boolean inclusive;

    @Override
    public void initialize(EndDateAfterStartDate constraintAnnotation) {
        this.inclusive = constraintAnnotation.inclusive();
    }

    @Override
    public boolean isValid(DateRangeRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate startDate = value.startDate();
        LocalDate endDate = value.endDate();
        if (startDate == null || endDate == null) {
            return true;
        }
        return inclusive ? !endDate.isBefore(startDate) : endDate.isAfter(startDate);
    }
}
