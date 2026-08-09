package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.ExplanationDirection;
import com.kleos.transfers.domain.InjurySeverity;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.injury.entity.Injury;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
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
    static final int ESTABLISHED_STARTER_MINUTES = 2_500;
    static final int GK_STARTER_MINUTES = 2_500;
    static final int GK_DEFAULT_STARTER_MINUTES = 3_200;
    /** Prior-season minutes above which a club's keeper counts as a settled number one. */
    static final int GK_INCUMBENT_MINUTES = 2_000;
    /** Minutes an arriving keeper needs over the incumbent to be favoured for the gloves. */
    static final int GK_TAKEOVER_EDGE = 600;
    /** Minutes behind the incumbent at which the arrival is simply the backup. */
    static final int GK_BACKUP_EDGE = -400;
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
        double competitionMultiplier = competitionMultiplier(context, baseline, factors);

        int minutes = PredictionMath.clamp(
                (int) Math.round(baseline * ageMultiplier * injuryMultiplier * competitionMultiplier),
                MIN_MINUTES,
                MAX_MINUTES
        );
        return new Result(minutes, minutes, minutes, factors);
    }

    /**
     * Goalkeeping is winner-take-most: one shirt, and the keeper who owned it last season keeps it
     * unless the arrival clearly outplayed him. Whoever loses that contest drops to bench minutes,
     * so the outcome is set on the baseline rather than shaved off it by a competition multiplier.
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
        int starterCeiling = (int) Math.round(Math.max(recentWorkload, GK_DEFAULT_STARTER_MINUTES) * 0.90);

        int baseline;
        if (context.playerHistory().isEmpty()) {
            baseline = DEFAULT_MINUTES;
            factors.add(gkRoleFactor(
                    ExplanationDirection.NEUTRAL,
                    14,
                    "No prior GK seasons; using a neutral rotation baseline of " + DEFAULT_MINUTES + " minutes."
            ));
        } else if (incumbentMinutes < GK_INCUMBENT_MINUTES) {
            baseline = starterProfile ? Math.max(recentWorkload, GK_DEFAULT_STARTER_MINUTES) : recentWorkload;
            factors.add(gkRoleFactor(
                    starterProfile ? ExplanationDirection.POSITIVE : ExplanationDirection.NEUTRAL,
                    starterProfile ? 22 : 12,
                    "Recent workload ("
                            + recentWorkload
                            + " min) "
                            + (starterProfile ? "looks like a first-choice goalkeeper" : "is a rotation share")
                            + "; carrying it forward."
            ));
            factors.add(gkCompetitionFactor(
                    ExplanationDirection.POSITIVE,
                    "No settled number one at "
                            + context.targetClub().getName()
                            + " (top rival GK minutes: "
                            + incumbentMinutes
                            + ")"
                            + (depth.starterSlotVacated() ? " after the previous starter's exit" : "")
                            + ", so the shirt is there to be taken."
            ));
        } else {
            int edge = recentWorkload - incumbentMinutes;
            baseline = contestedGoalkeeperMinutes(edge, recentWorkload, starterCeiling);
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
                    edge >= GK_TAKEOVER_EDGE ? ExplanationDirection.POSITIVE : ExplanationDirection.NEGATIVE,
                    describeGoalkeeperContest(incumbent, edge, recentWorkload)
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

    /**
     * Ramps from bench minutes to starter minutes across the workload gap with the incumbent, so a
     * keeper who merely matches the number one still projects behind him.
     */
    private int contestedGoalkeeperMinutes(int edge, int recentWorkload, int starterCeiling) {
        if (edge >= GK_TAKEOVER_EDGE) {
            return starterCeiling;
        }
        double share = Math.min(1.0, Math.max(0.0,
                (edge - GK_BACKUP_EDGE) / (double) (GK_TAKEOVER_EDGE - GK_BACKUP_EDGE)));
        int contested = (int) Math.round(GK_BENCH_MINUTES + (share * (starterCeiling - GK_BENCH_MINUTES)));
        return Math.min(contested, Math.max(recentWorkload, GK_BENCH_MINUTES));
    }

    private String describeGoalkeeperContest(
            SquadDepthAnalyzer.Contender incumbent,
            int edge,
            int recentWorkload
    ) {
        String name = incumbent == null ? "the incumbent" : incumbent.name();
        int incumbentMinutes = incumbent == null ? 0 : incumbent.minutes();
        if (edge >= GK_TAKEOVER_EDGE) {
            return "Played " + edge + " more minutes than " + name + " (" + incumbentMinutes
                    + " min), so he is favoured to take the gloves.";
        }
        if (edge >= GK_BACKUP_EDGE) {
            return name + " kept the shirt last season (" + incumbentMinutes + " min) and is staying, so"
                    + " the arrival splits minutes at best rather than walking into the XI.";
        }
        return "First-choice keeper " + name + " (" + incumbentMinutes + " min) is staying, so an arrival on "
                + recentWorkload + " minutes projects as the backup and only plays through injury or rotation.";
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

    /**
     * Depth-chart competition at the player's exact role: how many starting slots that role owns,
     * how many are already held by more established rivals, and whether the window vacated one.
     */
    private double competitionMultiplier(
            PredictionContext context,
            int baselineMinutes,
            List<ExplanationFactor> factors
    ) {
        Position role = resolvePosition(context);
        SquadDepthAnalyzer.Assessment depth =
                SquadDepthAnalyzer.analyze(context, role, baselineMinutes);

        double multiplier = opennessMultiplier(depth.openness());
        multiplier *= crowdingMultiplier(depth.crowding());
        multiplier = PredictionMath.clamp(multiplier, MIN_COMPETITION_MULTIPLIER, MAX_COMPETITION_MULTIPLIER);

        factors.add(competitionFactor(context, depth, multiplier));
        if (depth.starterSlotVacated()) {
            multiplier = applyVacancy(context, depth, baselineMinutes, multiplier, factors);
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
