package com.kleos.transfers.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

/**
 * Ensures {@link DateRangeRequest#endDate()} is strictly after {@link DateRangeRequest#startDate()}.
 */
public class EndDateAfterStartDateValidator implements ConstraintValidator<EndDateAfterStartDate, DateRangeRequest> {

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
        return endDate.isAfter(startDate);
    }
}
