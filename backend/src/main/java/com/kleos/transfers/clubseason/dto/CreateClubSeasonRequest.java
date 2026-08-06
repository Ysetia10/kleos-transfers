package com.kleos.transfers.clubseason.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload for creating a club-season historical record.
 */
public record CreateClubSeasonRequest(
        @NotNull UUID clubId,
        @NotNull UUID seasonId,
        @NotNull UUID tournamentId
) {
}
