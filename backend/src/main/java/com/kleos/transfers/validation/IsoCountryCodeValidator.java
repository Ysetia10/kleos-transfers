package com.kleos.transfers.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Set;

/**
 * Constraint validator backed by the JDK ISO 3166-1 alpha-3 country-code list.
 */
public class IsoCountryCodeValidator implements ConstraintValidator<IsoCountryCode, String> {

    private static final Set<String> ISO_ALPHA_3_CODES = Set.copyOf(
            Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA3)
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || ISO_ALPHA_3_CODES.contains(value);
    }
}
