package com.kleos.transfers.clubseason.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a club-season historical record.
 */
public record ClubSeasonResponse(
        UUID id,
        UUID clubId,
        String clubName,
        UUID seasonId,
        String seasonLabel,
        UUID tournamentId,
        String tournamentName,
        Instant createdAt,
        Instant updatedAt
) {
}
