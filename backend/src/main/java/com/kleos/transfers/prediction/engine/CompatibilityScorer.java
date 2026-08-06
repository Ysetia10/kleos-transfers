package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.contract.entity.Contract;
import com.kleos.transfers.domain.ExplanationDirection;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * v0 transfer-compatibility score (0–100) with factor breakdown.
 */
@Component
public class CompatibilityScorer {

    public record Result(BigDecimal score, List<ExplanationFactor> factors) {
    }

    public Result score(PredictionContext context, int predictedMinutes) {
        List<ExplanationFactor> factors = new ArrayList<>();
        double score = 55.0;

        int age = context.ageAtSeasonStart();
        if (age >= 21 && age <= 29) {
            score += 12;
            factors.add(factor(
                    FactorCodes.AGE_PROFILE,
                    "Age fit",
                    ExplanationDirection.POSITIVE,
                    12,
                    "Age " + age + " fits a typical adaptation window for a new club."
            ));
        } else if (age >= 33) {
            score -= 15;
            factors.add(factor(
                    FactorCodes.AGE_PROFILE,
                    "Age fit",
                    ExplanationDirection.NEGATIVE,
                    15,
                    "Age " + age + " raises adaptation and durability risk after a move."
            ));
        } else {
            factors.add(factor(
                    FactorCodes.AGE_PROFILE,
                    "Age fit",
                    ExplanationDirection.NEUTRAL,
                    6,
                    "Age " + age + " is workable but outside the core peak window."
            ));
        }

        if (predictedMinutes >= 2_200) {
            score += 10;
            factors.add(factor(
                    FactorCodes.RECENT_MINUTES,
                    "Likely role",
                    ExplanationDirection.POSITIVE,
                    10,
                    "Projected minutes suggest a meaningful role rather than fringe minutes."
            ));
        } else if (predictedMinutes < 1_200) {
            score -= 12;
            factors.add(factor(
                    FactorCodes.RECENT_MINUTES,
                    "Likely role",
                    ExplanationDirection.NEGATIVE,
                    12,
                    "Low projected minutes imply a difficult role battle or limited trust."
            ));
        }

        if (!context.recentInjuries().isEmpty()) {
            score -= 10;
            factors.add(factor(
                    FactorCodes.INJURY_BURDEN,
                    "Availability risk",
                    ExplanationDirection.NEGATIVE,
                    10,
                    "Recent injury history reduces expected adaptation continuity."
            ));
        }

        ExplanationFactor contractFactor = contractPressureFactor(context);
        score += signedImpact(contractFactor);
        factors.add(contractFactor);

        Optional<ExplanationFactor> leagueFactor = leagueTransition(context);
        if (leagueFactor.isPresent()) {
            ExplanationFactor factor = leagueFactor.get();
            score += signedImpact(factor);
            factors.add(factor);
        }

        if (context.targetClubSeason().isEmpty()) {
            score -= 5;
            factors.add(factor(
                    FactorCodes.DATA_COVERAGE,
                    "Target club context",
                    ExplanationDirection.NEGATIVE,
                    5,
                    "No ClubSeason row for the target club yet — league/context fit is weaker."
            ));
        }

        BigDecimal clamped = PredictionMath.clamp(
                PredictionMath.bd(score),
                PredictionMath.bd(0),
                PredictionMath.bd(100)
        );
        return new Result(clamped, factors);
    }

    private ExplanationFactor contractPressureFactor(PredictionContext context) {
        List<Contract> contracts = context.playerContracts();
        if (contracts.isEmpty()) {
            return factor(
                    FactorCodes.CONTRACT_PRESSURE,
                    "Contract pressure",
                    ExplanationDirection.NEUTRAL,
                    4,
                    "No contract windows on file — expiry pressure unknown."
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
                    "Contract pressure",
                    ExplanationDirection.POSITIVE,
                    8,
                    "Current deal expires within ~"
                            + monthsLeft
                            + " months — transfers and role commitments are more plausible."
            );
        }
        if (monthsLeft >= 36) {
            return factor(
                    FactorCodes.CONTRACT_PRESSURE,
                    "Contract pressure",
                    ExplanationDirection.NEGATIVE,
                    6,
                    "Long remaining contract (~"
                            + monthsLeft
                            + " months) can raise acquisition friction."
            );
        }
        return factor(
                FactorCodes.CONTRACT_PRESSURE,
                "Contract pressure",
                ExplanationDirection.NEUTRAL,
                4,
                "Mid-length remaining contract (~" + monthsLeft + " months)."
        );
    }

    private Optional<ExplanationFactor> leagueTransition(PredictionContext context) {
        Optional<PlayerSeason> recent = context.mostRecentSeason();
        if (recent.isEmpty()) {
            return Optional.empty();
        }
        String fromCountry = recent.get().getClub().getCountryCode();
        String toCountry = context.targetClub().getCountryCode();
        if (fromCountry.equals(toCountry)) {
            return Optional.of(factor(
                    FactorCodes.LEAGUE_TRANSITION,
                    "League transition",
                    ExplanationDirection.POSITIVE,
                    8,
                    "Staying in "
                            + toCountry
                            + " reduces adaptation friction versus a cross-border move."
            ));
        }
        return Optional.of(factor(
                FactorCodes.LEAGUE_TRANSITION,
                "League transition",
                ExplanationDirection.NEGATIVE,
                8,
                "Moving from "
                        + fromCountry
                        + " to "
                        + toCountry
                        + " introduces a league/style transition."
        ));
    }

    private double signedImpact(ExplanationFactor factor) {
        return switch (factor.direction()) {
            case POSITIVE -> factor.impact().doubleValue();
            case NEGATIVE -> -factor.impact().doubleValue();
            case NEUTRAL -> 0;
        };
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
}
