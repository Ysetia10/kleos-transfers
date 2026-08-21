package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.ExplanationDirection;
import com.kleos.transfers.domain.InjurySeverity;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.injury.entity.Injury;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Minutes predictor: recent workload adjusted for age, injury, and squad competition.
 *
 * <p>Goalkeepers use a separate starter/backup pathway — outfield competition rules over-penalize
 * clubs that list multiple GKs when only one typically starts.
 */
@Component
public class MinutesPredictor {

    static final int DEFAULT_MINUTES = 1_800;
    static final int MAX_MINUTES = 3_800;
    static final int MIN_MINUTES = 0;
    static final int ESTABLISHED_STARTER_MINUTES = 2_000;
    static final int GK_STARTER_MINUTES = 2_500;
    static final int GK_DEFAULT_STARTER_MINUTES = 3_200;
    /** Prior-season minutes above which a club's keeper counts as a settled number one. */
    static final int GK_INCUMBENT_MINUTES = 2_000;
    /** Minutes an arriving keeper needs over the incumbent to be favoured for the gloves. */
    static final int GK_TAKEOVER_EDGE = 400;
    /** Soft takeover when the incumbent is only a borderline starter. */
    static final int GK_SOFT_TAKEOVER_EDGE = 250;
    static final int GK_SOFT_INCUMBENT_CEILING = 2_700;
    /** League minutes a settled backup keeper typically picks up through rotation and injury. */
    static final int GK_BENCH_MINUTES = 500;
    static final int REPLACEMENT_BASELINE_MINUTES = 1_500;
    static final double REPLACEMENT_FLOOR_MULTIPLIER = 0.92;
    static final double MIN_COMPETITION_MULTIPLIER = 0.30;
    static final double MAX_COMPETITION_MULTIPLIER = 1.12;

    public record Result(int minutes, int minutesLow, int minutesHigh, List<ExplanationFactor> factors) {
    }

    public Result predict(PredictionContext context) {
        Position position = resolvePosition(context);
        if (position == Position.GK) {
            return withInterval(predictGoalkeeper(context), context);
        }
        return withInterval(predictOutfield(context), context);
    }

    private Result withInterval(Result point, PredictionContext context) {
        double band = uncertaintyBand(context);
        int low = PredictionMath.clamp(
                (int) Math.round(point.minutes() * (1.0 - band)),
                MIN_MINUTES,
                point.minutes()
        );
        int high = PredictionMath.clamp(
                (int) Math.round(point.minutes() * (1.0 + band)),
                point.minutes(),
                MAX_MINUTES
        );
        List<ExplanationFactor> factors = new ArrayList<>(point.factors());
        factors.add(new ExplanationFactor(
                FactorCodes.MINUTES_INTERVAL,
                "Minutes interval",
                ExplanationDirection.NEUTRAL,
                PredictionMath.bd(band * 100),
                "Expected minutes band "
                        + low
                        + "–"
                        + high
                        + " (±"
                        + Math.round(band * 100)
                        + "% around the point estimate from history depth and risk signals)."
        ));
        return new Result(point.minutes(), low, high, factors);
    }

    /**
     * Relative half-width for the minutes confidence interval (0.12–0.35).
     */
    private double uncertaintyBand(PredictionContext context) {
        double band = 0.18;
        int historySeasons = Math.min(3, context.playerHistory().size());
        if (historySeasons == 0) {
            band += 0.12;
        } else if (historySeasons == 1) {
            band += 0.06;
        } else if (historySeasons >= 3) {
            band -= 0.04;
        }
        if (!context.recentInjuries().isEmpty()) {
            band += 0.06;
        }
        if (context.targetClubSeason().isEmpty()) {
            band += 0.04;
        }
        if (context.targetClubSquad().isEmpty()) {
            band += 0.03;
        }
        if (!SquadDepthAnalyzer.hasPreciseRoles(context, resolvePosition(context))) {
            band += 0.03;
        }
        return Math.min(0.35, Math.max(0.12, band));
    }

    private Result predictOutfield(PredictionContext context) {
        List<ExplanationFactor> factors = new ArrayList<>();
        int baseline = baselineMinutes(context, factors);
        double ageMultiplier = ageMultiplier(context.ageAtSeasonStart(), factors);
        double injuryMultiplier = injuryMultiplier(context, factors);
        Position role = resolvePosition(context);
        SquadDepthAnalyzer.Assessment depth =
                SquadDepthAnalyzer.analyze(context, role, baseline);
        double competitionMultiplier = competitionMultiplier(context, baseline, depth, factors);

        int minutes = PredictionMath.clamp(
                (int) Math.round(baseline * ageMultiplier * injuryMultiplier * competitionMultiplier),
                MIN_MINUTES,
                MAX_MINUTES
        );
        // Rule A/D (v0.5+): absolute floor when a starter slot is vacated (incl. low-history walk-ins).
        minutes = applyVacancyMinutesFloor(context, depth, baseline, minutes, factors);
        // v0.6: open depth without a tagged leaver — still a walk-into-XI pattern.
        minutes = applyOpenDepthWalkInFloor(context, depth, baseline, minutes, factors);
        // Rule B (v0.5): established arrivals rarely collapse below ~1600' historically.
        minutes = applyEstablishedArrivalFloor(baseline, minutes, factors);
        return new Result(minutes, minutes, minutes, factors);
    }

    /**
     * When the destination role's starter left in-window, do not let competition haircuts collapse
     * the projection toward zero. Covers both established replacements and low-history walk-ins
     * (loan/youth → vacated shirt), which dominate CM/ITA under-prediction tails.
     */
    private int applyVacancyMinutesFloor(
            PredictionContext context,
            SquadDepthAnalyzer.Assessment depth,
            int baselineMinutes,
            int minutes,
            List<ExplanationFactor> factors
    ) {
        if (!depth.starterSlotVacated()) {
            return minutes;
        }
        int leaverMinutes = depth.topDeparture().map(SquadDepthAnalyzer.Contender::minutes).orElse(2_200);
        int vacancyFloor;
        if (baselineMinutes >= 1_000) {
            vacancyFloor = PredictionMath.clamp(
                    Math.max(2_000, (int) Math.round(baselineMinutes * 0.80)),
                    2_000,
                    2_900
            );
        } else {
            // Low prior minutes but a starter shirt opened — inherit ~55% of the leaver's load.
            vacancyFloor = PredictionMath.clamp(
                    (int) Math.round(leaverMinutes * 0.55),
                    1_400,
                    2_400
            );
        }
        if (vacancyFloor <= minutes) {
            return minutes;
        }
        factors.add(new ExplanationFactor(
                FactorCodes.SQUAD_VACANCY,
                "Replacement minutes floor",
                ExplanationDirection.POSITIVE,
                PredictionMath.bd(12),
                "Vacated starter slot at "
                        + context.targetClub().getName()
                        + " ("
                        + leaverMinutes
                        + " min departed): projecting at least "
                        + vacancyFloor
                        + " minutes"
                        + (baselineMinutes < 1_000
                                ? " for a low-history walk-in."
                                : " for a signing with " + baselineMinutes + " recent minutes.")
        ));
        return PredictionMath.clamp(vacancyFloor, MIN_MINUTES, MAX_MINUTES);
    }

    /**
     * When transfer coverage misses a departure but the remaining depth chart is thin, low-history
     * arrivals still often walk into meaningful minutes (esp. ITA/ESP CM).
     */
    private int applyOpenDepthWalkInFloor(
            PredictionContext context,
            SquadDepthAnalyzer.Assessment depth,
            int baselineMinutes,
            int minutes,
            List<ExplanationFactor> factors
    ) {
        if (depth.starterSlotVacated() || baselineMinutes >= 1_200 || !depth.openDepthForWalkIn()) {
            return minutes;
        }
        int walkInFloor = baselineMinutes < 400 ? 1_500 : 1_700;
        if (walkInFloor <= minutes) {
            return minutes;
        }
        factors.add(new ExplanationFactor(
                FactorCodes.SQUAD_VACANCY,
                "Open depth walk-in floor",
                ExplanationDirection.POSITIVE,
                PredictionMath.bd(10),
                "Thin remaining depth at "
                        + resolvePosition(context)
                        + " for "
                        + context.targetClub().getName()
                        + " with no locked starter ahead — projecting at least "
                        + walkInFloor
                        + " minutes."
        ));
        return PredictionMath.clamp(walkInFloor, MIN_MINUTES, MAX_MINUTES);
    }

    /**
     * Club-changers with ≥ {@link #ESTABLISHED_STARTER_MINUTES} prior minutes still median ~1.5–1.8k
     * even into locked roles — keep a hard floor so competition cannot project near-zero.
     */
    private int applyEstablishedArrivalFloor(
            int baselineMinutes,
            int minutes,
            List<ExplanationFactor> factors
    ) {
        if (baselineMinutes < ESTABLISHED_STARTER_MINUTES) {
            return minutes;
        }
        int establishedFloor = 1_600;
        if (establishedFloor <= minutes) {
            return minutes;
        }
        factors.add(new ExplanationFactor(
                FactorCodes.SQUAD_COMPETITION,
                "Established arrival floor",
                ExplanationDirection.POSITIVE,
                PredictionMath.bd(8),
                "Club-changers with ≥"
                        + ESTABLISHED_STARTER_MINUTES
                        + " recent minutes rarely drop below ~"
                        + establishedFloor
                        + " in the next season."
        ));
        return PredictionMath.clamp(establishedFloor, MIN_MINUTES, MAX_MINUTES);
    }

    /**
     * Goalkeeping is winner-take-most. Empirically (top-5 club-changing GKs):
     * <ul>
     *   <li>Starter arrives and prior #1 stays → median ~360' (backup)</li>
     *   <li>Starter arrives and prior #1 leaves → median ~2790' (takes the shirt)</li>
     *   <li>Backup arrives and prior #1 stays → median ~270'</li>
     *   <li>Backup arrives and prior #1 leaves → median ~2040'</li>
     * </ul>
     * So when a settled number one remains, outcomes are binary (takeover vs bench), not a soft
     * minutes split. An vacated starter shirt opens the role even if a residual rival remains.
     */
    private Result predictGoalkeeper(PredictionContext context) {
        List<ExplanationFactor> factors = new ArrayList<>();
        int recentWorkload = recentWorkloadMinutes(context);
        SquadDepthAnalyzer.Assessment depth =
                SquadDepthAnalyzer.analyze(context, Position.GK, recentWorkload);
        SquadDepthAnalyzer.Contender incumbent = depth.rivals().stream()
                .max(Comparator.comparingInt(SquadDepthAnalyzer.Contender::minutes))
                .orElse(null);
        int incumbentMinutes = incumbent == null ? 0 : incumbent.minutes();
        boolean starterProfile = recentWorkload >= GK_STARTER_MINUTES;
        boolean substantialStarterShare = recentWorkload >= GK_INCUMBENT_MINUTES;
        boolean shirtOpen = incumbentMinutes < GK_INCUMBENT_MINUTES || depth.starterSlotVacated();
        int starterCeiling = (int) Math.round(Math.max(recentWorkload, GK_DEFAULT_STARTER_MINUTES) * 0.90);

        int baseline;
        if (context.playerHistory().isEmpty()) {
            baseline = DEFAULT_MINUTES;
            factors.add(gkRoleFactor(
                    ExplanationDirection.NEUTRAL,
                    14,
                    "No prior GK seasons; using a neutral rotation baseline of " + DEFAULT_MINUTES + " minutes."
            ));
        } else if (shirtOpen) {
            if (starterProfile || substantialStarterShare || recentWorkload >= 1_200) {
                // Meaningful prior into an open shirt → project as #1.
                baseline = Math.max(recentWorkload, GK_DEFAULT_STARTER_MINUTES);
            } else if (depth.starterSlotVacated()) {
                // Thin prior into a vacated shirt still often becomes the default starter.
                baseline = Math.max(recentWorkload, 2_200);
            } else {
                baseline = Math.max(recentWorkload, GK_BENCH_MINUTES);
            }
            factors.add(gkRoleFactor(
                    starterProfile || substantialStarterShare || depth.starterSlotVacated() || recentWorkload >= 1_200
                            ? ExplanationDirection.POSITIVE
                            : ExplanationDirection.NEUTRAL,
                    starterProfile || substantialStarterShare || recentWorkload >= 1_200 ? 24 : 14,
                    "Recent workload ("
                            + recentWorkload
                            + " min) "
                            + (starterProfile || substantialStarterShare || recentWorkload >= 1_200
                                    ? "is first-choice / high-share level"
                                    : depth.starterSlotVacated()
                                            ? "is modest, but the starting shirt is vacant"
                                            : "is a rotation share")
                            + "; projecting accordingly."
            ));
            factors.add(gkCompetitionFactor(
                    ExplanationDirection.POSITIVE,
                    "No settled number one blocking the gloves at "
                            + context.targetClub().getName()
                            + " (top rival GK minutes: "
                            + incumbentMinutes
                            + ")"
                            + (depth.starterSlotVacated() ? " after the previous starter's exit" : "")
                            + ", so the shirt is there to be taken."
            ));
        } else {
            // Rule 1: settled #1 stays — takeover with a clear edge (softer vs borderline incumbents).
            int edge = recentWorkload - incumbentMinutes;
            int neededEdge = incumbentMinutes <= GK_SOFT_INCUMBENT_CEILING && starterProfile
                    ? GK_SOFT_TAKEOVER_EDGE
                    : GK_TAKEOVER_EDGE;
            if (edge >= neededEdge) {
                baseline = starterCeiling;
            } else {
                baseline = GK_BENCH_MINUTES;
            }
            factors.add(gkRoleFactor(
                    starterProfile ? ExplanationDirection.POSITIVE : ExplanationDirection.NEGATIVE,
                    18,
                    "Recent workload ("
                            + recentWorkload
                            + " min) "
                            + (starterProfile ? "is first-choice level" : "is below first-choice level")
                            + " at his previous club."
            ));
            factors.add(gkCompetitionFactor(
                    edge >= neededEdge ? ExplanationDirection.POSITIVE : ExplanationDirection.NEGATIVE,
                    describeGoalkeeperContest(incumbent, edge, neededEdge, recentWorkload)
            ));
        }

        factors.add(new ExplanationFactor(
                FactorCodes.RECENT_MINUTES,
                "Recent minutes baseline",
                baseline >= 2_000 ? ExplanationDirection.POSITIVE : ExplanationDirection.NEUTRAL,
                PredictionMath.bd(Math.min(25, 8 + baseline / 200.0)),
                "GK minutes baseline set to " + baseline + " after role classification."
        ));

        double ageMultiplier = goalkeeperAgeMultiplier(context.ageAtSeasonStart(), factors);
        double injuryMultiplier = injuryMultiplier(context, factors);

        int minutes = PredictionMath.clamp(
                (int) Math.round(baseline * ageMultiplier * injuryMultiplier),
                MIN_MINUTES,
                MAX_MINUTES
        );
        return new Result(minutes, minutes, minutes, factors);
    }

    private String describeGoalkeeperContest(
            SquadDepthAnalyzer.Contender incumbent,
            int edge,
            int neededEdge,
            int recentWorkload
    ) {
        String name = incumbent == null ? "the incumbent" : incumbent.name();
        int incumbentMinutes = incumbent == null ? 0 : incumbent.minutes();
        if (edge >= neededEdge) {
            return "Played " + edge + " more minutes than " + name + " (" + incumbentMinutes
                    + " min), so he is favoured to take the gloves.";
        }
        return "First-choice keeper " + name + " (" + incumbentMinutes
                + " min) is staying, so an arrival on " + recentWorkload
                + " minutes projects as the backup (~" + GK_BENCH_MINUTES
                + " min) unless injury or cup rotation intervenes.";
    }

    private ExplanationFactor gkRoleFactor(ExplanationDirection direction, int impact, String detail) {
        return new ExplanationFactor(
                FactorCodes.GK_ROLE,
                "Goalkeeper role",
                direction,
                PredictionMath.bd(impact),
                detail
        );
    }

    private ExplanationFactor gkCompetitionFactor(ExplanationDirection direction, String detail) {
        return new ExplanationFactor(
                FactorCodes.SQUAD_COMPETITION,
                "Squad competition",
                direction,
                PredictionMath.bd(20),
                detail
        );
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

        int average = recentWorkloadMinutes(context);
        int seasons = Math.min(3, history.size());
        factors.add(new ExplanationFactor(
                FactorCodes.RECENT_MINUTES,
                "Recent minutes baseline",
                average >= 2_000 ? ExplanationDirection.POSITIVE : ExplanationDirection.NEUTRAL,
                PredictionMath.bd(Math.min(25, 8 + average / 200.0)),
                "Weighted recent minutes baseline of "
                        + average
                        + " across the last "
                        + seasons
                        + " club-season record(s) (most recent season weighted highest)."
        ));
        return average;
    }

    private int recentWorkloadMinutes(PredictionContext context) {
        List<PlayerSeason> history = context.playerHistory();
        if (history.isEmpty()) {
            return 0;
        }
        int seasons = Math.min(3, history.size());
        if (seasons == 1) {
            return history.get(0).getMinutesPlayed();
        }
        if (seasons == 2) {
            return (int) Math.round(
                    history.get(0).getMinutesPlayed() * 0.65
                            + history.get(1).getMinutesPlayed() * 0.35
            );
        }
        return (int) Math.round(
                history.get(0).getMinutesPlayed() * 0.50
                        + history.get(1).getMinutesPlayed() * 0.30
                        + history.get(2).getMinutesPlayed() * 0.20
        );
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
            multiplier = 0.95;
            direction = ExplanationDirection.NEGATIVE;
            detail = "Age " + age + " — slight reduction for late-career workload management.";
        } else {
            multiplier = 0.80;
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

    private double goalkeeperAgeMultiplier(int age, List<ExplanationFactor> factors) {
        double multiplier;
        ExplanationDirection direction;
        String detail;
        if (age < 21) {
            multiplier = 0.80;
            direction = ExplanationDirection.NEGATIVE;
            detail = "Age " + age + " — young goalkeepers rarely lock a starter role immediately.";
        } else if (age <= 33) {
            multiplier = 1.03;
            direction = ExplanationDirection.POSITIVE;
            detail = "Age " + age + " is inside the typical goalkeeper prime.";
        } else if (age <= 37) {
            multiplier = 0.96;
            direction = ExplanationDirection.NEUTRAL;
            detail = "Age " + age + " — still viable for a starting GK with a mild workload trim.";
        } else {
            multiplier = 0.85;
            direction = ExplanationDirection.NEGATIVE;
            detail = "Age " + age + " — veteran GK minutes tempered for durability risk.";
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

        LocalDate asOf = context.season().getStartDate();
        // Club-changers often bounce back after a completed spell; only haircut material unavailability
        // still overlapping the lookback into the new season.
        double weightedDays = 0;
        boolean ongoing = false;
        boolean severeNearStart = false;
        int counted = 0;
        for (Injury injury : injuries) {
            if (injury.isOngoing() || injury.getEndDate() == null) {
                ongoing = true;
                counted++;
                weightedDays += injury.getDaysOut() == null ? 60 : injury.getDaysOut();
                if (injury.getSeverity() == InjurySeverity.SEVERE) {
                    severeNearStart = true;
                }
                continue;
            }
            long daysSinceReturn = java.time.temporal.ChronoUnit.DAYS.between(injury.getEndDate(), asOf);
            if (daysSinceReturn >= 90) {
                // Fully recovered before season start — keep for explanation but ignore in multiplier.
                continue;
            }
            counted++;
            double weight = daysSinceReturn >= 45 ? 0.35 : daysSinceReturn >= 14 ? 0.65 : 1.0;
            int daysOut = injury.getDaysOut() == null ? 45 : injury.getDaysOut();
            weightedDays += daysOut * weight;
            if (injury.getSeverity() == InjurySeverity.SEVERE && daysSinceReturn < 45) {
                severeNearStart = true;
            }
        }

        if (counted == 0) {
            factors.add(new ExplanationFactor(
                    FactorCodes.INJURY_BURDEN,
                    "Injury burden",
                    ExplanationDirection.POSITIVE,
                    PredictionMath.bd(6),
                    "Prior injury spell(s) ended well before season start — treating availability as recovered."
            ));
            return 1.02;
        }

        double multiplier = 1.0;
        if (ongoing) {
            multiplier -= 0.18;
        }
        if (severeNearStart) {
            multiplier -= 0.10;
        }
        multiplier -= Math.min(0.22, weightedDays / 500.0);
        multiplier = Math.max(0.55, multiplier);

        factors.add(new ExplanationFactor(
                FactorCodes.INJURY_BURDEN,
                "Injury burden",
                ExplanationDirection.NEGATIVE,
                PredictionMath.bd((1.0 - multiplier) * 50),
                "Recent injury load: "
                        + counted
                        + " material spell(s), ~"
                        + Math.round(weightedDays)
                        + " weighted days out"
                        + (ongoing ? ", including an ongoing spell" : "")
                        + "."
        ));
        return multiplier;
    }

    /**
     * Depth-chart competition at the player's exact role: how many starting slots that role owns,
     * how many are already held by more established rivals, and whether the window vacated one.
     */
    private double competitionMultiplier(
            PredictionContext context,
            int baselineMinutes,
            SquadDepthAnalyzer.Assessment depth,
            List<ExplanationFactor> factors
    ) {
        double multiplier = opennessMultiplier(depth.openness());
        multiplier *= crowdingMultiplier(depth.crowding());
        // Established arrivals: competition floor 0.50 (Rule C) — softens near-zero haircuts.
        double minMult = baselineMinutes >= ESTABLISHED_STARTER_MINUTES ? 0.50 : MIN_COMPETITION_MULTIPLIER;
        multiplier = PredictionMath.clamp(multiplier, minMult, MAX_COMPETITION_MULTIPLIER);

        factors.add(competitionFactor(context, depth, multiplier));
        if (depth.starterSlotVacated()) {
            multiplier = applyVacancy(context, depth, baselineMinutes, multiplier, factors);
            multiplier = PredictionMath.clamp(multiplier, minMult, MAX_COMPETITION_MULTIPLIER);
        }
        if (!depth.preciseRoles()) {
            factors.add(rolePrecisionFactor(context, depth));
        }
        return multiplier;
    }

    private double opennessMultiplier(double openness) {
        if (openness >= 1.0) {
            return 1.05;
        }
        if (openness > 0) {
            return 0.78 + (0.27 * openness);
        }
        return Math.max(0.30, 0.78 / (1.0 + (1.10 * -openness)));
    }

    /**
     * Small extra haircut for squads that stack bodies at a role even below the subject.
     */
    private double crowdingMultiplier(double crowding) {
        double excess = Math.max(0, crowding - 1.15);
        return 1.0 - Math.min(0.12, 0.08 * excess);
    }

    /**
     * A departing starter leaves a slot the club has to fill, so a replacement signing with real
     * minutes behind him is unlikely to be squeezed out.
     */
    private double applyVacancy(
            PredictionContext context,
            SquadDepthAnalyzer.Assessment depth,
            int baselineMinutes,
            double multiplier,
            List<ExplanationFactor> factors
    ) {
        double boosted = multiplier * (1.0 + Math.min(0.12, 0.12 * depth.vacated()));
        if (baselineMinutes >= REPLACEMENT_BASELINE_MINUTES) {
            boosted = Math.max(boosted, REPLACEMENT_FLOOR_MULTIPLIER);
        }
        boosted = Math.min(MAX_COMPETITION_MULTIPLIER, boosted);

        String leaver = depth.topDeparture()
                .map(departure -> departure.name() + " (" + departure.minutes() + " min)")
                .orElse("a first-choice starter");
        factors.add(new ExplanationFactor(
                FactorCodes.SQUAD_VACANCY,
                "Vacated starting slot",
                ExplanationDirection.POSITIVE,
                PredictionMath.bd(Math.abs(boosted - multiplier) * 100 + 8),
                context.targetClub().getName()
                        + " is losing "
                        + leaver
                        + " at "
                        + depth.role()
                        + " this window, so the arriving replacement inherits starting minutes rather than"
                        + " queueing behind the incumbent."
        ));
        return boosted;
    }

    private ExplanationFactor competitionFactor(
            PredictionContext context,
            SquadDepthAnalyzer.Assessment depth,
            double multiplier
    ) {
        String scope = depth.preciseRoles()
                ? "at " + depth.role()
                : "across the " + depth.role() + " line";
        if (depth.rivals().isEmpty()) {
            return new ExplanationFactor(
                    FactorCodes.SQUAD_COMPETITION,
                    "Squad competition",
                    ExplanationDirection.POSITIVE,
                    PredictionMath.bd(10),
                    "No rivals " + scope + " in the post-transfer squad at " + context.targetClub().getName() + "."
            );
        }

        String blocker = depth.topBlocker()
                .map(rival -> " Ahead of him: " + rival.name() + " (" + rival.minutes() + " min at " + rival.role() + ").")
                .orElse(" No rival at that role is more established than he is.");
        boolean negative = multiplier < 1.0;
        return new ExplanationFactor(
                FactorCodes.SQUAD_COMPETITION,
                "Squad competition",
                negative ? ExplanationDirection.NEGATIVE : ExplanationDirection.POSITIVE,
                PredictionMath.bd(Math.abs(1.0 - multiplier) * 60 + 6),
                String.format(
                        "%.1f starting slot(s) %s, contested by %.1f weighted rival(s), %.1f of them ranked ahead.%s",
                        depth.slots(),
                        scope,
                        depth.contested(),
                        depth.blocked(),
                        blocker
                )
        );
    }

    private ExplanationFactor rolePrecisionFactor(
            PredictionContext context,
            SquadDepthAnalyzer.Assessment depth
    ) {
        return new ExplanationFactor(
                FactorCodes.ROLE_PRECISION,
                "Role precision",
                ExplanationDirection.NEUTRAL,
                PredictionMath.bd(10),
                context.targetClub().getName()
                        + " squad roles are only recorded at line level for the prior season, so competition is"
                        + " judged across the whole line instead of the exact "
                        + depth.role()
                        + " slot."
        );
    }


    private static Position resolvePosition(PredictionContext context) {
        return context.mostRecentSeason()
                .map(PlayerSeason::getPrimaryPosition)
                .orElse(context.player().getPrimaryPosition());
    }
}
