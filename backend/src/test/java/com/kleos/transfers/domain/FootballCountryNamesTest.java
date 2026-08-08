package com.kleos.transfers.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FootballCountryNamesTest {

    @Test
    void resolvesFifaCodesAndDisplayNames() {
        assertThat(FootballCountryNames.codesMatchingQuery("ESP")).containsExactly("ESP");
        assertThat(FootballCountryNames.codesMatchingQuery("Spain")).contains("ESP");
        assertThat(FootballCountryNames.codesMatchingQuery("france")).contains("FRA");
        assertThat(FootballCountryNames.codesMatchingQuery("England")).contains("ENG");
    }

    @Test
    void resolvesCommonAliases() {
        assertThat(FootballCountryNames.codesMatchingQuery("Holland")).contains("NED");
        assertThat(FootballCountryNames.codesMatchingQuery("Turkey")).contains("TUR");
    }

    @Test
    void ignoresVeryShortQueriesForNameMatching() {
        assertThat(FootballCountryNames.codesMatchingQuery("sp")).isEmpty();
    }
}
