package com.kleos.transfers.prediction.dto;

import com.kleos.transfers.domain.ExplanationDirection;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * One explainability factor returned with a prediction.
 */
public record ExplanationResponse(
        UUID id,
        String factorCode,
        String label,
        ExplanationDirection direction,
        BigDecimal impact,
        String detail,
        Integer sortOrder
) {
}
