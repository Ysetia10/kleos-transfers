package com.kleos.transfers.clubseason.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload for updating a club-season historical record.
 *
 * <p>Club and season may be corrected; the active uniqueness slot follows the pair.
 */
public record UpdateClubSeasonRequest(
        @NotNull UUID clubId,
        @NotNull UUID seasonId,
        @NotNull UUID tournamentId
) {
}
