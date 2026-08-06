package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.ExplanationDirection;
import java.math.BigDecimal;

/**
 * One explainable contribution produced by a predictor.
 *
 * @param impact absolute contribution magnitude on a 0–100 style scale
 */
public record ExplanationFactor(
        String code,
        String label,
        ExplanationDirection direction,
        BigDecimal impact,
        String detail
) {
}
