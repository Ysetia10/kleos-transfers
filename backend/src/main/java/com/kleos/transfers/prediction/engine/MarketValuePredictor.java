package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.ExplanationDirection;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * v0 market-value estimate from age band and predicted contribution.
 *
 * <p>This is intentionally coarse — real valuations need fee comps and contract data.
 */
@Component
public class MarketValuePredictor {

    public record Result(BigDecimal valueEur, List<ExplanationFactor> factors) {
    }

    public Result predict(
            PredictionContext context,
            int predictedMinutes,
            BigDecimal predictedGoals,
            BigDecimal predictedAssists
    ) {
        double ageBase = ageBase(context.ageAtSeasonStart());
        double contribution = predictedGoals.doubleValue() * 2.5
                + predictedAssists.doubleValue() * 1.8
                + predictedMinutes / 250.0;
        double value = ageBase * (0.55 + Math.min(contribution, 40) / 40.0);
        BigDecimal rounded = PredictionMath.bd(Math.round(value / 100_000.0) * 100_000.0);

        ExplanationFactor factor = new ExplanationFactor(
                FactorCodes.PERFORMANCE_VALUE,
                "Performance-linked value",
                ExplanationDirection.NEUTRAL,
                PredictionMath.bd(12),
                "Rough end-of-season value from age band (€"
                        + (long) ageBase
                        + " base) and predicted contribution ("
                        + String.format("%.1f", contribution)
                        + " units)."
        );
        return new Result(rounded, List.of(factor));
    }

    private double ageBase(int age) {
        if (age < 20) {
            return 8_000_000;
        }
        if (age <= 24) {
            return 25_000_000;
        }
        if (age <= 28) {
            return 35_000_000;
        }
        if (age <= 31) {
            return 18_000_000;
        }
        return 6_000_000;
    }
}
