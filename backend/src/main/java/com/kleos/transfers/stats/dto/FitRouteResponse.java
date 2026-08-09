package com.kleos.transfers.stats.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One high-fit hypothetical (or previously simulated) transfer route for Trending.
 */
public record FitRouteResponse(
        UUID playerId,
        String playerName,
        String playerPhotoUrl,
        UUID fromClubId,
        String fromClubName,
        UUID toClubId,
        String toClubName,
        UUID seasonId,
        String seasonLabel,
        BigDecimal compatibilityScore,
        Integer predictedMinutes,
        UUID predictionId,
        String source
) {
}
