package com.kleos.transfers.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FootballNationalityCodesTest {

    @Test
    void acceptsFootballAssociationCodesRejectedByIso() {
        assertThat(FootballNationalityCodes.isValid("ENG")).isTrue();
        assertThat(FootballNationalityCodes.isValid("SCO")).isTrue();
        assertThat(FootballNationalityCodes.isValid("WAL")).isTrue();
        assertThat(FootballNationalityCodes.isValid("NIR")).isTrue();
        assertThat(FootballNationalityCodes.isValid("GER")).isTrue();
        assertThat(FootballNationalityCodes.isValid("NED")).isTrue();
        assertThat(FootballNationalityCodes.isValid("SUI")).isTrue();
    }

    @Test
    void rejectsIsoOnlyAndUnknownCodes() {
        assertThat(FootballNationalityCodes.isValid("GBR")).isFalse();
        assertThat(FootballNationalityCodes.isValid("DEU")).isFalse();
        assertThat(FootballNationalityCodes.isValid("NLD")).isFalse();
        assertThat(FootballNationalityCodes.isValid("XXX")).isFalse();
        assertThat(FootballNationalityCodes.isValid(null)).isFalse();
    }
}
