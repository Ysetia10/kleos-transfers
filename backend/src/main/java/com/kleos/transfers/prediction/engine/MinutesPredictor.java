package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.ExplanationDirection;
import com.kleos.transfers.domain.InjurySeverity;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.injury.entity.Injury;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * v0 minutes predictor: recent workload adjusted for age, injury, and squad competition.
 */
@Component
public class MinutesPredictor {

    static final int DEFAULT_MINUTES = 1_800;
    static final int MAX_MINUTES = 3_800;
    static final int MIN_MINUTES = 0;

    public record Result(int minutes, List<ExplanationFactor> factors) {
    }

    public Result predict(PredictionContext context) {
        List<ExplanationFactor> factors = new ArrayList<>();
        int baseline = baselineMinutes(context, factors);
        double ageMultiplier = ageMultiplier(context.ageAtSeasonStart(), factors);
        double injuryMultiplier = injuryMultiplier(context, factors);
        double competitionMultiplier = competitionMultiplier(context, factors);

        int minutes = PredictionMath.clamp(
                (int) Math.round(baseline * ageMultiplier * injuryMultiplier * competitionMultiplier),
                MIN_MINUTES,
                MAX_MINUTES
        );
        return new Result(minutes, factors);
    }

    private int baselineMinutes(PredictionContext context, List<ExplanationFactor> factors) {
        List<PlayerSeason> history = context.playerHistory();
        if (history.isEmpty()) {
            factors.add(new ExplanationFactor(
                    FactorCodes.RECENT_MINUTES,
                    "Limited playing-time history",
                    ExplanationDirection.NEUTRAL,
                    PredictionMath.bd(12),
                    "No prior PlayerSeason rows; using a squad-rotation baseline of "
                            + DEFAULT_MINUTES
                            + " minutes."
            ));
            return DEFAULT_MINUTES;
        }

        int seasons = Math.min(3, history.size());
        int total = 0;
        for (int i = 0; i < seasons; i++) {
            total += history.get(i).getMinutesPlayed();
        }
        int average = total / seasons;
        factors.add(new ExplanationFactor(
                FactorCodes.RECENT_MINUTES,
                "Recent minutes baseline",
                average >= 2_000 ? ExplanationDirection.POSITIVE : ExplanationDirection.NEUTRAL,
                PredictionMath.bd(Math.min(25, 8 + average / 200.0)),
                "Averaged "
                        + average
                        + " minutes across the last "
                        + seasons
                        + " club-season record(s)."
        ));
        return average;
    }

    private double ageMultiplier(int age, List<ExplanationFactor> factors) {
        double multiplier;
        ExplanationDirection direction;
        String detail;
        if (age < 20) {
            multiplier = 0.85;
            direction = ExplanationDirection.NEGATIVE;
            detail = "Age " + age + " — minutes tempered while the player is still developing.";
        } else if (age <= 28) {
            multiplier = 1.05;
            direction = ExplanationDirection.POSITIVE;
            detail = "Age " + age + " is inside the typical peak availability window.";
        } else if (age <= 32) {
            multiplier = 0.92;
            direction = ExplanationDirection.NEGATIVE;
            detail = "Age " + age + " — slight reduction for late-career workload management.";
        } else {
            multiplier = 0.75;
            direction = ExplanationDirection.NEGATIVE;
            detail = "Age " + age + " — larger reduction for veteran minutes expectation.";
        }
        factors.add(new ExplanationFactor(
                FactorCodes.AGE_PROFILE,
                "Age profile",
                direction,
                PredictionMath.bd(Math.abs(1.0 - multiplier) * 40),
                detail
        ));
        return multiplier;
    }

    private double injuryMultiplier(PredictionContext context, List<ExplanationFactor> factors) {
        List<Injury> injuries = context.recentInjuries();
        if (injuries.isEmpty()) {
            factors.add(new ExplanationFactor(
                    FactorCodes.INJURY_BURDEN,
                    "Injury burden",
                    ExplanationDirection.POSITIVE,
                    PredictionMath.bd(8),
                    "No injury spells recorded in the last 12 months before the season."
            ));
            return 1.05;
        }

        boolean ongoing = injuries.stream().anyMatch(Injury::isOngoing);
        int daysOut = injuries.stream()
                .mapToInt(injury -> injury.getDaysOut() == null ? 45 : injury.getDaysOut())
                .sum();
        boolean severe = injuries.stream().anyMatch(i -> i.getSeverity() == InjurySeverity.SEVERE);

        double multiplier = 1.0;
        if (ongoing) {
            multiplier -= 0.25;
        }
        if (severe) {
            multiplier -= 0.15;
        }
        multiplier -= Math.min(0.30, daysOut / 400.0);
        multiplier = Math.max(0.45, multiplier);

        factors.add(new ExplanationFactor(
                FactorCodes.INJURY_BURDEN,
                "Injury burden",
                ExplanationDirection.NEGATIVE,
                PredictionMath.bd((1.0 - multiplier) * 50),
                "Recent injury load: "
                        + injuries.size()
                        + " spell(s), ~"
                        + daysOut
                        + " days out"
                        + (ongoing ? ", including an ongoing spell" : "")
                        + "."
        ));
        return multiplier;
    }

    private double competitionMultiplier(PredictionContext context, List<ExplanationFactor> factors) {
        Position position = context.mostRecentSeason()
                .map(PlayerSeason::getPrimaryPosition)
                .orElse(context.player().getPrimaryPosition());
        Set<Position> group = PositionGroups.groupOf(position);

        long rivals = context.targetClubSquad().stream()
                .filter(ps -> !ps.getPlayer().getId().equals(context.player().getId()))
                .filter(ps -> group.contains(ps.getPrimaryPosition()))
                .count();

        if (rivals == 0) {
            factors.add(new ExplanationFactor(
                    FactorCodes.SQUAD_COMPETITION,
                    "Squad competition",
                    ExplanationDirection.POSITIVE,
                    PredictionMath.bd(10),
                    "No recorded same-position rivals at the target club for this season yet."
            ));
            return 1.05;
        }

        double multiplier = rivals == 1 ? 0.92 : rivals == 2 ? 0.82 : 0.70;
        factors.add(new ExplanationFactor(
                FactorCodes.SQUAD_COMPETITION,
                "Squad competition",
                ExplanationDirection.NEGATIVE,
                PredictionMath.bd(rivals * 8.0),
                rivals
                        + " player(s) already logged in the same position group at "
                        + context.targetClub().getName()
                        + " this season."
        ));
        return multiplier;
    }
}
