package com.kleos.transfers.stats.domain;

/**
 * Supported domestic leagues for leaderboard aggregations.
 */
public enum LeagueCode {
    PREMIER_LEAGUE("Premier League"),
    LA_LIGA("La Liga");

    private final String tournamentName;

    LeagueCode(String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public String tournamentName() {
        return tournamentName;
    }
}
