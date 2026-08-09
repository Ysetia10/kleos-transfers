package com.kleos.transfers.prediction.engine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full output of one prediction engine pass — metrics plus ordered explanation factors.
 */
public record EngineResult(
        int predictedMinutes,
        BigDecimal predictedGoals,
        BigDecimal predictedAssists,
        BigDecimal predictedXg,
        BigDecimal predictedXa,
        BigDecimal predictedMarketValueEur,
        BigDecimal compatibilityScore,
        CompatibilityBreakdown compatibilityBreakdown,
        BigDecimal confidenceScore,
        List<ExplanationFactor> factors
) {
}
