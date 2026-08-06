package com.kleos.transfers.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validates football season labels used as stable identity keys.
 *
 * <p>Accepted forms:
 * <ul>
 *   <li>{@code YYYY/YY} — European-style seasons (e.g. {@code 2024/25})</li>
 *   <li>{@code YYYY} — calendar-year seasons (e.g. {@code 2024})</li>
 * </ul>
 *
 * <p>For {@code YYYY/YY}, the second year must be the calendar successor of the first
 * (modulo 100), so {@code 2024/25} is valid and {@code 2024/26} is not.
 */
public class SeasonLabelValidator implements ConstraintValidator<SeasonLabel, String> {

    private static final Pattern CALENDAR_YEAR = Pattern.compile("^\\d{4}$");
    private static final Pattern EUROPEAN = Pattern.compile("^(\\d{4})/(\\d{2})$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String label = value.trim();
        if (CALENDAR_YEAR.matcher(label).matches()) {
            return true;
        }
        var matcher = EUROPEAN.matcher(label);
        if (!matcher.matches()) {
            return false;
        }
        int startYear = Integer.parseInt(matcher.group(1));
        int endSuffix = Integer.parseInt(matcher.group(2));
        return endSuffix == ((startYear + 1) % 100);
    }
}
