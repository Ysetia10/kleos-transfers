package com.kleos.transfers.prediction.engine;

import java.math.BigDecimal;

/**
 * Five fit dimensions used by the simulator mockup and API.
 */
public record CompatibilityBreakdown(
        BigDecimal system,
        BigDecimal role,
        BigDecimal tempo,
        BigDecimal league,
        BigDecimal manager
) {
}
