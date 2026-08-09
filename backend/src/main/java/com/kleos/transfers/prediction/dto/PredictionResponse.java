package com.kleos.transfers.prediction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full prediction payload including metrics, scores, explanations, and optional evaluation.
 */
public record PredictionResponse(
        UUID id,
        UUID runId,
        String modelVersion,
        UUID playerId,
        String playerName,
        UUID targetClubId,
        String targetClubName,
        UUID seasonId,
        String seasonLabel,
        Integer predictedMinutes,
        BigDecimal predictedGoals,
        BigDecimal predictedAssists,
        BigDecimal predictedXg,
        BigDecimal predictedXa,
        BigDecimal predictedMarketValueEur,
        BigDecimal compatibilityScore,
        CompatibilityBreakdownResponse compatibilityBreakdown,
        BigDecimal confidenceScore,
        List<ExplanationResponse> explanations,
        EvaluationResponse evaluation,
        Instant createdAt,
        Instant updatedAt
) {
}
