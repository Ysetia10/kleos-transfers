package com.kleos.transfers.validation;

import com.kleos.transfers.domain.FootballNationalityCodes;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;

/**
 * Accepts FIFA nationality codes, normalizing case before lookup.
 */
public class FootballNationalityCodeValidator
        implements ConstraintValidator<FootballNationalityCode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return FootballNationalityCodes.isValid(value.trim().toUpperCase(Locale.ROOT));
    }
}
