package com.kleos.transfers.stats.domain;

/**
 * Supported domestic leagues for leaderboard aggregations (top-five European).
 */
public enum LeagueCode {
    PREMIER_LEAGUE("Premier League"),
    LA_LIGA("La Liga"),
    BUNDESLIGA("Bundesliga"),
    SERIE_A("Serie A"),
    LIGUE_1("Ligue 1");

    private final String tournamentName;

    LeagueCode(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public String tournamentName() {
        return tournamentName;
    }
}
