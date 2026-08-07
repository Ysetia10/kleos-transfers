package com.kleos.transfers.stats.dto;

import java.util.UUID;

/**
 * One row on a league scorers / assisters board.
 */
public record LeaderboardEntryResponse(
        UUID playerId,
        String playerName,
        UUID clubId,
        String clubName,
        long goals,
        long assists,
        long appearances,
        long minutesPlayed,
        long seasonsPlayed
) {
}
