package com.kleos.transfers.prediction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Post-season evaluation of a prediction against observed outcomes.
 */
public record EvaluationResponse(
        UUID id,
        Integer actualMinutes,
        Integer actualGoals,
        Integer actualAssists,
        BigDecimal actualXg,
        BigDecimal actualXa,
        Integer minutesError,
        BigDecimal goalsError,
        BigDecimal assistsError,
        BigDecimal xgError,
        BigDecimal xaError,
        Instant evaluatedAt
) {
}
