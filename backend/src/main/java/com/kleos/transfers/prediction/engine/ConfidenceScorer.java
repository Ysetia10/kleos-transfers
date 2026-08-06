package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.ExplanationDirection;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * v0 confidence score (0–100) based on how complete the input context is.
 */
@Component
public class ConfidenceScorer {

    public record Result(BigDecimal score, List<ExplanationFactor> factors) {
    }

    public Result score(PredictionContext context) {
        List<ExplanationFactor> factors = new ArrayList<>();
        double score = 35.0;

        int historySize = context.playerHistory().size();
        if (historySize == 0) {
            score -= 10;
            factors.add(factor(
                    ExplanationDirection.NEGATIVE,
                    10,
                    "No PlayerSeason history — rates fall back to defaults."
            ));
        } else if (historySize == 1) {
            score += 15;
            factors.add(factor(
                    ExplanationDirection.NEUTRAL,
                    10,
                    "Only one historical season available."
            ));
        } else {
            score += 25;
            factors.add(factor(
                    ExplanationDirection.POSITIVE,
                    18,
                    historySize + " historical PlayerSeason rows support stabler rates."
            ));
        }

        if (context.targetClubSeason().isPresent()) {
            score += 12;
            factors.add(factor(
                    ExplanationDirection.POSITIVE,
                    10,
                    "Target club has a ClubSeason row for the predicted season."
            ));
        } else {
            score -= 8;
            factors.add(factor(
                    ExplanationDirection.NEGATIVE,
                    8,
                    "Missing ClubSeason for the target club reduces context quality."
            ));
        }

        if (!context.playerContracts().isEmpty()) {
            score += 8;
        } else {
            score -= 4;
            factors.add(factor(
                    ExplanationDirection.NEGATIVE,
                    4,
                    "No contracts on file — expiry context is incomplete."
            ));
        }

        if (!context.recentInjuries().isEmpty() || context.playerHistory().size() >= 2) {
            score += 5;
        }

        int age = context.ageAtSeasonStart();
        if (age < 18 || age > 36) {
            score -= 10;
            factors.add(factor(
                    ExplanationDirection.NEGATIVE,
                    10,
                    "Age " + age + " is outside the range where the v0 heuristic is most reliable."
            ));
        }

        BigDecimal clamped = PredictionMath.clamp(
                PredictionMath.bd(score),
                PredictionMath.bd(0),
                PredictionMath.bd(100)
        );
        return new Result(clamped, factors);
    }

    private ExplanationFactor factor(ExplanationDirection direction, double impact, String detail) {
        return new ExplanationFactor(
                FactorCodes.DATA_COVERAGE,
                "Data coverage",
                direction,
                PredictionMath.bd(impact),
                detail
        );
    }
}
