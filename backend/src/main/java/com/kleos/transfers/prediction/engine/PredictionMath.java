package com.kleos.transfers.prediction.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Shared numeric helpers for the heuristic predictors.
 */
final class PredictionMath {

    private PredictionMath() {
    }

    static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static BigDecimal per90(Number countingStat, int minutes) {
        if (minutes <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(countingStat.doubleValue())
                .multiply(BigDecimal.valueOf(90))
                .divide(BigDecimal.valueOf(minutes), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal scaleByMinutes(BigDecimal per90, int minutes) {
        return per90
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(90), 2, RoundingMode.HALF_UP);
    }
}
