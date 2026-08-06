package com.kleos.transfers.prediction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to run a transfer what-if prediction for one player/club/season scenario.
 */
public record CreatePredictionRequest(
        @NotNull UUID playerId,
        @NotNull UUID targetClubId,
        @NotNull UUID seasonId,
        @Size(max = 255) String note
) {
}
