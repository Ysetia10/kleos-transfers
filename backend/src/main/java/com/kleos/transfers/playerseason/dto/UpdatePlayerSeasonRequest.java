package com.kleos.transfers.playerseason.dto;

import com.kleos.transfers.domain.Position;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload for updating a player-season performance record.
 */
public record UpdatePlayerSeasonRequest(
        @NotNull UUID playerId,
        @NotNull UUID clubId,
        @NotNull UUID seasonId,
        @NotNull @Min(0) Integer appearances,
        @NotNull @Min(0) Integer minutesPlayed,
        @NotNull @Min(0) Integer goals,
        @NotNull @Min(0) Integer assists,
        @NotNull @DecimalMin("0.0") BigDecimal xg,
        @NotNull @DecimalMin("0.0") BigDecimal xa,
        @NotNull Position primaryPosition
) {
}
