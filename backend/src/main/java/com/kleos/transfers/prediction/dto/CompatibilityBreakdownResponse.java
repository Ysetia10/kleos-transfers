package com.kleos.transfers.prediction.dto;

import java.math.BigDecimal;

/**
 * Dimensional transfer-fit scores (0–100) that roll up into {@code compatibilityScore}.
 */
public record CompatibilityBreakdownResponse(
        BigDecimal system,
        BigDecimal role,
        BigDecimal tempo,
        BigDecimal league,
        BigDecimal manager
) {
}
