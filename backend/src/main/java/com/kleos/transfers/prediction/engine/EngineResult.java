package com.kleos.transfers.prediction.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Deterministic engine output for one player → club → season scenario.
 */
public record EngineResult(
        int predictedMinutes,
        int predictedMinutesLow,
        int predictedMinutesHigh,
        BigDecimal predictedGoals,
        BigDecimal predictedAssists,
        BigDecimal predictedMarketValueEur,
        BigDecimal compatibilityScore,
        CompatibilityBreakdown compatibilityBreakdown,
        BigDecimal confidenceScore,
        List<ExplanationFactor> factors
) {
}
