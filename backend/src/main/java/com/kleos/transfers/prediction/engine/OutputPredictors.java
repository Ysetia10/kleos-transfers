package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.ExplanationDirection;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * v0 counting-stat and expected-stat predictors scaled from historical per-90 rates.
 */
@Component
public class OutputPredictors {

    public record CountingResult(BigDecimal value, List<ExplanationFactor> factors) {
    }

    public CountingResult predictGoals(PredictionContext context, int predictedMinutes) {
        RateSnapshot rate = rateSnapshot(context);
        BigDecimal per90 = rate.goalsPer90();
        Position position = context.player().getPrimaryPosition();
        if (PositionGroups.isAttacker(position) && per90.compareTo(PredictionMath.bd(0.20)) < 0) {
            per90 = PredictionMath.bd(0.25);
        }
        if (position == Position.GK) {
            per90 = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal goals = PredictionMath.scaleByMinutes(per90, predictedMinutes);
        List<ExplanationFactor> factors = new ArrayList<>();
        factors.add(new ExplanationFactor(
                FactorCodes.SCORING_RATE,
                "Scoring rate",
                per90.compareTo(PredictionMath.bd(0.30)) >= 0
                        ? ExplanationDirection.POSITIVE
                        : ExplanationDirection.NEUTRAL,
                PredictionMath.bd(Math.min(20, per90.doubleValue() * 25)),
                "Applied "
                        + per90
                        + " goals/90 from "
                        + rate.sourceLabel()
                        + " to "
                        + predictedMinutes
                        + " predicted minutes."
        ));
        return new CountingResult(goals, factors);
    }

    public CountingResult predictAssists(PredictionContext context, int predictedMinutes) {
        RateSnapshot rate = rateSnapshot(context);
        BigDecimal per90 = rate.assistsPer90();
        if (context.player().getPrimaryPosition() == Position.GK) {
            per90 = BigDecimal.ZERO.setScale(2);
        }
        BigDecimal assists = PredictionMath.scaleByMinutes(per90, predictedMinutes);
        List<ExplanationFactor> factors = List.of(new ExplanationFactor(
                FactorCodes.CREATION_RATE,
                "Creation rate",
                per90.compareTo(PredictionMath.bd(0.20)) >= 0
                        ? ExplanationDirection.POSITIVE
                        : ExplanationDirection.NEUTRAL,
                PredictionMath.bd(Math.min(18, per90.doubleValue() * 25)),
                "Applied "
                        + per90
                        + " assists/90 from "
                        + rate.sourceLabel()
                        + " to "
                        + predictedMinutes
                        + " predicted minutes."
        ));
        return new CountingResult(assists, factors);
    }

    public CountingResult predictXg(PredictionContext context, int predictedMinutes) {
        RateSnapshot rate = rateSnapshot(context);
        BigDecimal per90 = rate.xgPer90();
        if (context.player().getPrimaryPosition() == Position.GK) {
            per90 = BigDecimal.ZERO.setScale(2);
        }
        BigDecimal xg = PredictionMath.scaleByMinutes(per90, predictedMinutes);
        List<ExplanationFactor> factors = List.of(new ExplanationFactor(
                FactorCodes.XG_RATE,
                "xG rate",
                ExplanationDirection.NEUTRAL,
                PredictionMath.bd(Math.min(15, per90.doubleValue() * 20)),
                "Projected "
                        + xg
                        + " xG from a "
                        + per90
                        + " xG/90 historical rate."
        ));
        return new CountingResult(xg, factors);
    }

    public CountingResult predictXa(PredictionContext context, int predictedMinutes) {
        RateSnapshot rate = rateSnapshot(context);
        BigDecimal per90 = rate.xaPer90();
        if (context.player().getPrimaryPosition() == Position.GK) {
            per90 = BigDecimal.ZERO.setScale(2);
        }
        BigDecimal xa = PredictionMath.scaleByMinutes(per90, predictedMinutes);
        List<ExplanationFactor> factors = List.of(new ExplanationFactor(
                FactorCodes.XA_RATE,
                "xA rate",
                ExplanationDirection.NEUTRAL,
                PredictionMath.bd(Math.min(15, per90.doubleValue() * 20)),
                "Projected "
                        + xa
                        + " xA from a "
                        + per90
                        + " xA/90 historical rate."
        ));
        return new CountingResult(xa, factors);
    }

    private RateSnapshot rateSnapshot(PredictionContext context) {
        List<PlayerSeason> history = context.playerHistory();
        if (history.isEmpty()) {
            return RateSnapshot.defaults();
        }
        int seasons = Math.min(3, history.size());
        int minutes = 0;
        double goals = 0;
        double assists = 0;
        double xg = 0;
        double xa = 0;
        for (int i = 0; i < seasons; i++) {
            PlayerSeason row = history.get(i);
            minutes += row.getMinutesPlayed();
            goals += row.getGoals();
            assists += row.getAssists();
            xg += row.getXg().doubleValue();
            xa += row.getXa().doubleValue();
        }
        if (minutes == 0) {
            return RateSnapshot.defaults();
        }
        return new RateSnapshot(
                PredictionMath.per90(goals, minutes),
                PredictionMath.per90(assists, minutes),
                PredictionMath.per90(xg, minutes),
                PredictionMath.per90(xa, minutes),
                "the last " + seasons + " PlayerSeason record(s)"
        );
    }

    private record RateSnapshot(
            BigDecimal goalsPer90,
            BigDecimal assistsPer90,
            BigDecimal xgPer90,
            BigDecimal xaPer90,
            String sourceLabel
    ) {
        static RateSnapshot defaults() {
            return new RateSnapshot(
                    PredictionMath.bd(0.15),
                    PredictionMath.bd(0.10),
                    PredictionMath.bd(0.18),
                    PredictionMath.bd(0.12),
                    "position-neutral defaults (no history)"
            );
        }
    }
}
