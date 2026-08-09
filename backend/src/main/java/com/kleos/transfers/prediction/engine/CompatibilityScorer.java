package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.contract.entity.Contract;
import com.kleos.transfers.domain.ExplanationDirection;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Transfer-compatibility score (0–100) with SYSTEM / ROLE / TEMPO / LEAGUE / MANAGER dimensions.
 */
@Component
public class CompatibilityScorer {

    public record Result(
            BigDecimal score,
            CompatibilityBreakdown breakdown,
            List<ExplanationFactor> factors
    ) {
    }

    public Result score(PredictionContext context, int predictedMinutes) {
        List<ExplanationFactor> factors = new ArrayList<>();

        DimensionScore system = scoreSystem(context, factors);
        DimensionScore role = scoreRole(predictedMinutes, factors);
        DimensionScore tempo = scoreTempo(context, factors);
        DimensionScore league = scoreLeague(context, factors);
        DimensionScore manager = scoreManager(context, factors);

        CompatibilityBreakdown breakdown = new CompatibilityBreakdown(
                system.value(),
                role.value(),
                tempo.value(),
                league.value(),
                manager.value()
        );

        BigDecimal aggregate = PredictionMath.bd(
                (system.value().doubleValue()
                        + role.value().doubleValue()
                        + tempo.value().doubleValue()
                        + league.value().doubleValue()
                        + manager.value().doubleValue())
                        / 5.0
        ).setScale(2, RoundingMode.HALF_UP);

        return new Result(aggregate, breakdown, factors);
    }

    private DimensionScore scoreSystem(PredictionContext context, List<ExplanationFactor> factors) {
        double score = 62;
        if (context.targetClubSeason().isEmpty()) {
            score -= 12;
            factors.add(factor(
                    FactorCodes.DATA_COVERAGE,
                    "System fit",
                    ExplanationDirection.NEGATIVE,
                    12,
                    "No recent ClubSeason for the target club — tactical system context is thin."
            ));
        } else {
            score += 8;
            factors.add(factor(
                    FactorCodes.DATA_COVERAGE,
                    "System fit",
                    ExplanationDirection.POSITIVE,
                    8,
                    "Target club has recent season context for system comparison."
            ));
        }

        long peers = context.targetClubSquad().stream()
                .filter(row -> row.getPrimaryPosition() == context.player().getPrimaryPosition())
                .count();
        if (peers >= 3) {
            score -= 10;
            factors.add(factor(
                    FactorCodes.SQUAD_COMPETITION,
                    "System depth",
                    ExplanationDirection.NEGATIVE,
                    10,
                    "Several squad peers share the same primary position — system minutes are contested."
            ));
        } else if (peers <= 1) {
            score += 8;
            factors.add(factor(
                    FactorCodes.SQUAD_COMPETITION,
                    "System depth",
                    ExplanationDirection.POSITIVE,
                    8,
                    "Limited positional overlap in the prior squad suggests clearer system space."
            ));
        } else {
            factors.add(factor(
                    FactorCodes.SQUAD_COMPETITION,
                    "System depth",
                    ExplanationDirection.NEUTRAL,
                    5,
                    "Moderate positional overlap at the target club."
            ));
        }

        return clamped(score);
    }

    private DimensionScore scoreRole(int predictedMinutes, List<ExplanationFactor> factors) {
        double score = 58;
        if (predictedMinutes >= 2_200) {
            score += 22;
            factors.add(factor(
                    FactorCodes.RECENT_MINUTES,
                    "Role fit",
                    ExplanationDirection.POSITIVE,
                    22,
                    "Projected minutes suggest a meaningful starting role."
            ));
        } else if (predictedMinutes < 1_200) {
            score -= 20;
            factors.add(factor(
                    FactorCodes.RECENT_MINUTES,
                    "Role fit",
                    ExplanationDirection.NEGATIVE,
                    20,
                    "Low projected minutes imply a difficult role battle or rotation risk."
            ));
        } else {
            score += 6;
            factors.add(factor(
                    FactorCodes.RECENT_MINUTES,
                    "Role fit",
                    ExplanationDirection.NEUTRAL,
                    6,
                    "Projected minutes sit in a rotation / shared-role band."
            ));
        }
        return clamped(score);
    }

    private DimensionScore scoreTempo(PredictionContext context, List<ExplanationFactor> factors) {
        double score = 60;
        int age = context.ageAtSeasonStart();
        if (age >= 21 && age <= 29) {
            score += 18;
            factors.add(factor(
                    FactorCodes.AGE_PROFILE,
                    "Tempo fit",
                    ExplanationDirection.POSITIVE,
                    18,
                    "Age " + age + " fits a typical high-tempo adaptation window."
            ));
        } else if (age >= 33) {
            score -= 18;
            factors.add(factor(
                    FactorCodes.AGE_PROFILE,
                    "Tempo fit",
                    ExplanationDirection.NEGATIVE,
                    18,
                    "Age " + age + " raises durability risk in a high-tempo environment."
            ));
        } else {
            factors.add(factor(
                    FactorCodes.AGE_PROFILE,
                    "Tempo fit",
                    ExplanationDirection.NEUTRAL,
                    6,
                    "Age " + age + " is workable but outside the core peak tempo window."
            ));
        }

        if (!context.recentInjuries().isEmpty()) {
            score -= 14;
            factors.add(factor(
                    FactorCodes.INJURY_BURDEN,
                    "Tempo availability",
                    ExplanationDirection.NEGATIVE,
                    14,
                    "Recent injury history reduces expected tempo continuity after a move."
            ));
        } else {
            score += 6;
            factors.add(factor(
                    FactorCodes.INJURY_BURDEN,
                    "Tempo availability",
                    ExplanationDirection.POSITIVE,
                    6,
                    "No recent injuries on file — tempo continuity looks cleaner."
            ));
        }
        return clamped(score);
    }

    private DimensionScore scoreLeague(PredictionContext context, List<ExplanationFactor> factors) {
        double score = 64;
        Optional<PlayerSeason> recent = context.mostRecentSeason();
        if (recent.isEmpty()) {
            score -= 8;
            factors.add(factor(
                    FactorCodes.LEAGUE_TRANSITION,
                    "League fit",
                    ExplanationDirection.NEGATIVE,
                    8,
                    "No prior club season to judge league transition risk."
            ));
            return clamped(score);
        }

        String fromCountry = recent.get().getClub().getCountryCode();
        String toCountry = context.targetClub().getCountryCode();
        if (fromCountry.equals(toCountry)) {
            score += 18;
            factors.add(factor(
                    FactorCodes.LEAGUE_TRANSITION,
                    "League fit",
                    ExplanationDirection.POSITIVE,
                    18,
                    "Staying in " + toCountry + " reduces league adaptation friction."
            ));
        } else {
            score -= 14;
            factors.add(factor(
                    FactorCodes.LEAGUE_TRANSITION,
                    "League fit",
                    ExplanationDirection.NEGATIVE,
                    14,
                    "Moving from " + fromCountry + " to " + toCountry + " is a league/style transition."
            ));
        }
        return clamped(score);
    }

    private DimensionScore scoreManager(PredictionContext context, List<ExplanationFactor> factors) {
        double score = 58;
        if (context.targetManagerName().isPresent()) {
            score += 12;
            factors.add(factor(
                    FactorCodes.MANAGER_CONTEXT,
                    "Manager fit",
                    ExplanationDirection.POSITIVE,
                    12,
                    "Target club has a recorded manager ("
                            + context.targetManagerName().get()
                            + ") for recent season context."
            ));
        } else {
            score -= 8;
            factors.add(factor(
                    FactorCodes.MANAGER_CONTEXT,
                    "Manager fit",
                    ExplanationDirection.NEGATIVE,
                    8,
                    "No manager appointment on file for the target club — coaching fit is uncertain."
            ));
        }

        ExplanationFactor contractFactor = contractPressureFactor(context);
        score += signedImpact(contractFactor);
        factors.add(contractFactor);
        return clamped(score);
    }

    private ExplanationFactor contractPressureFactor(PredictionContext context) {
        List<Contract> contracts = context.playerContracts();
        if (contracts.isEmpty()) {
            return factor(
                    FactorCodes.CONTRACT_PRESSURE,
                    "Manager / deal friction",
                    ExplanationDirection.NEUTRAL,
                    4,
                    "No contract windows on file — acquisition friction unknown."
            );
        }
        LocalDate seasonStart = context.season().getStartDate();
        Contract active = contracts.stream()
                .filter(c -> !c.getStartDate().isAfter(seasonStart) && c.getEndDate().isAfter(seasonStart))
                .findFirst()
                .orElse(contracts.getFirst());
        long monthsLeft = ChronoUnit.MONTHS.between(seasonStart, active.getEndDate());
        if (monthsLeft <= 12) {
            return factor(
                    FactorCodes.CONTRACT_PRESSURE,
                    "Manager / deal friction",
                    ExplanationDirection.POSITIVE,
                    8,
                    "Current deal expires within ~" + monthsLeft + " months — moves are more plausible."
            );
        }
        if (monthsLeft >= 36) {
            return factor(
                    FactorCodes.CONTRACT_PRESSURE,
                    "Manager / deal friction",
                    ExplanationDirection.NEGATIVE,
                    6,
                    "Long remaining contract (~" + monthsLeft + " months) can raise acquisition friction."
            );
        }
        return factor(
                FactorCodes.CONTRACT_PRESSURE,
                "Manager / deal friction",
                ExplanationDirection.NEUTRAL,
                4,
                "Mid-length remaining contract (~" + monthsLeft + " months)."
        );
    }

    private double signedImpact(ExplanationFactor factor) {
        return switch (factor.direction()) {
            case POSITIVE -> factor.impact().doubleValue();
            case NEGATIVE -> -factor.impact().doubleValue();
            case NEUTRAL -> 0;
        };
    }

    private DimensionScore clamped(double score) {
        BigDecimal value = PredictionMath.clamp(
                PredictionMath.bd(score),
                PredictionMath.bd(0),
                PredictionMath.bd(100)
        );
        return new DimensionScore(value);
    }

    private ExplanationFactor factor(
            String code,
            String label,
            ExplanationDirection direction,
            double impact,
            String detail
    ) {
        return new ExplanationFactor(code, label, direction, PredictionMath.bd(impact), detail);
    }

    private record DimensionScore(BigDecimal value) {
    }
}
