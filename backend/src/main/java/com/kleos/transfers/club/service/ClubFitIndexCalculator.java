package com.kleos.transfers.club.service;

import com.kleos.transfers.domain.RecruitmentSignal;
import com.kleos.transfers.domain.TacticalSystem;
import com.kleos.transfers.domain.TempoProfile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Absolute club fit index (0–100), independent of any selected player.
 *
 * <p>Model version {@value #VERSION}: rewards manager context, stored/derived
 * tactical attributes, youth-minute opportunity, and top-five league membership.
 * Player→club compatibility remains on {@code Prediction.compatibilityScore}.
 */
public final class ClubFitIndexCalculator {

    public static final String VERSION = "absolute-v0.1";

    private static final Set<String> TOP_FIVE = Set.of("ENG", "ESP", "GER", "ITA", "FRA");

    private ClubFitIndexCalculator() {
    }

    public record Input(
            boolean managerPresent,
            TacticalSystem tacticalSystem,
            TempoProfile tempo,
            BigDecimal youthMinutesPct,
            String countryCode,
            boolean hasSquadSeason
    ) {
    }

    public record Result(
            BigDecimal fitIndex,
            RecruitmentSignal recruitmentSignal,
            String version
    ) {
    }

    public static Result compute(Input input) {
        double score = 38;

        if (input.managerPresent()) {
            score += 16;
        }
        if (input.tacticalSystem() != null) {
            score += 10;
        }
        if (input.tempo() != null) {
            score += 8;
            if (input.tempo() == TempoProfile.HIGH) {
                score += 2;
            }
        }
        if (input.youthMinutesPct() != null) {
            double youth = input.youthMinutesPct().doubleValue();
            score += Math.min(16.0, youth / 100.0 * 16.0);
        }
        if (input.countryCode() != null && TOP_FIVE.contains(input.countryCode().toUpperCase())) {
            score += 10;
        }
        if (input.hasSquadSeason()) {
            score += 6;
        }

        BigDecimal fitIndex = BigDecimal.valueOf(Math.max(0, Math.min(100, score)))
                .setScale(1, RoundingMode.HALF_UP);

        RecruitmentSignal signal = signalFor(fitIndex, input);
        return new Result(fitIndex, signal, VERSION);
    }

    private static RecruitmentSignal signalFor(BigDecimal fitIndex, Input input) {
        if (!input.managerPresent() && input.youthMinutesPct() == null && !input.hasSquadSeason()) {
            return RecruitmentSignal.UNKNOWN;
        }
        double fit = fitIndex.doubleValue();
        double youth = input.youthMinutesPct() == null ? 0 : input.youthMinutesPct().doubleValue();
        if (fit >= 70 && youth >= 18) {
            return RecruitmentSignal.HIGH;
        }
        if (fit >= 55) {
            return RecruitmentSignal.MEDIUM;
        }
        if (fit >= 40) {
            return RecruitmentSignal.LOW;
        }
        return RecruitmentSignal.UNKNOWN;
    }
}
