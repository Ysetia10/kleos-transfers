package com.kleos.transfers.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeasonLabelValidatorTest {

    private SeasonLabelValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SeasonLabelValidator();
    }

    @Test
    void acceptsEuropeanAndCalendarLabels() {
        assertThat(validator.isValid("2024/25", null)).isTrue();
        assertThat(validator.isValid("1999/00", null)).isTrue();
        assertThat(validator.isValid("2024", null)).isTrue();
        assertThat(validator.isValid(" 2025/26 ", null)).isTrue();
    }

    @Test
    void rejectsMalformedOrNonSuccessorLabels() {
        assertThat(validator.isValid("2024/26", null)).isFalse();
        assertThat(validator.isValid("24/25", null)).isFalse();
        assertThat(validator.isValid("2024-25", null)).isFalse();
        assertThat(validator.isValid("season-24", null)).isFalse();
        assertThat(validator.isValid("", null)).isFalse();
    }
}
