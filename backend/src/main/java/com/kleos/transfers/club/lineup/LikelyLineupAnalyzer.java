package com.kleos.transfers.club.lineup;

import com.kleos.transfers.club.dto.LikelyLineupPlacementResponse;
import com.kleos.transfers.club.dto.LikelyLineupResponse;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Infers a likely starting XI from squad minutes and precise positions — same heuristics as the
 * product pitch UI (formation pick + starter-minute thresholds).
 */
@Component
public class LikelyLineupAnalyzer {

    static final int STARTER_FLOOR = 900;
    static final double STARTER_SHARE = 0.45;

    enum FormationId {
        FOUR_THREE_THREE("4-3-3"),
        FOUR_TWO_THREE_ONE("4-2-3-1"),
        THREE_FOUR_THREE("3-4-3"),
        THREE_FIVE_TWO("3-5-2");

        final String label;

        FormationId(String label) {
            this.label = label;
        }
    }

    enum PitchSlotId {
        GK, LB, LCB, CB, RCB, RB, LWB, RWB, LDM, CDM, RDM, LCM, CM, RCM, LM, RM, LAM, CAM, RAM, LW, ST, RW
    }

    record PitchSlot(PitchSlotId id, double x, double y) {}

    enum SideBand {
        GK, CB, LEFT, RIGHT, MID, ATTACK
    }

    private static final Map<FormationId, List<PitchSlot>> FORMATIONS = Map.of(
            FormationId.FOUR_THREE_THREE, List.of(
                    slot(PitchSlotId.GK, 0.5, 0.08),
                    slot(PitchSlotId.LB, 0.12, 0.28),
                    slot(PitchSlotId.LCB, 0.36, 0.26),
                    slot(PitchSlotId.RCB, 0.64, 0.26),
                    slot(PitchSlotId.RB, 0.88, 0.28),
                    slot(PitchSlotId.LCM, 0.28, 0.5),
                    slot(PitchSlotId.CM, 0.5, 0.48),
                    slot(PitchSlotId.RCM, 0.72, 0.5),
                    slot(PitchSlotId.LW, 0.16, 0.74),
                    slot(PitchSlotId.ST, 0.5, 0.82),
                    slot(PitchSlotId.RW, 0.84, 0.74)
            ),
            FormationId.FOUR_TWO_THREE_ONE, List.of(
                    slot(PitchSlotId.GK, 0.5, 0.08),
                    slot(PitchSlotId.LB, 0.12, 0.28),
                    slot(PitchSlotId.LCB, 0.36, 0.26),
                    slot(PitchSlotId.RCB, 0.64, 0.26),
                    slot(PitchSlotId.RB, 0.88, 0.28),
                    slot(PitchSlotId.LDM, 0.38, 0.46),
                    slot(PitchSlotId.RDM, 0.62, 0.46),
                    slot(PitchSlotId.LAM, 0.18, 0.66),
                    slot(PitchSlotId.CAM, 0.5, 0.64),
                    slot(PitchSlotId.RAM, 0.82, 0.66),
                    slot(PitchSlotId.ST, 0.5, 0.84)
            ),
            FormationId.THREE_FOUR_THREE, List.of(
                    slot(PitchSlotId.GK, 0.5, 0.08),
                    slot(PitchSlotId.LCB, 0.22, 0.26),
                    slot(PitchSlotId.CB, 0.5, 0.24),
                    slot(PitchSlotId.RCB, 0.78, 0.26),
                    slot(PitchSlotId.LM, 0.14, 0.5),
                    slot(PitchSlotId.LCM, 0.38, 0.48),
                    slot(PitchSlotId.RCM, 0.62, 0.48),
                    slot(PitchSlotId.RM, 0.86, 0.5),
                    slot(PitchSlotId.LW, 0.2, 0.74),
                    slot(PitchSlotId.ST, 0.5, 0.82),
                    slot(PitchSlotId.RW, 0.8, 0.74)
            ),
            FormationId.THREE_FIVE_TWO, List.of(
                    slot(PitchSlotId.GK, 0.5, 0.08),
                    slot(PitchSlotId.LCB, 0.22, 0.26),
                    slot(PitchSlotId.CB, 0.5, 0.24),
                    slot(PitchSlotId.RCB, 0.78, 0.26),
                    slot(PitchSlotId.LWB, 0.1, 0.44),
                    slot(PitchSlotId.RWB, 0.9, 0.44),
                    slot(PitchSlotId.LCM, 0.32, 0.52),
                    slot(PitchSlotId.CM, 0.5, 0.5),
                    slot(PitchSlotId.RCM, 0.68, 0.52),
                    slot(PitchSlotId.ST, 0.38, 0.8),
                    slot(PitchSlotId.ST, 0.62, 0.8)
            )
    );

    private static final Set<Position> LATERAL = EnumSet.of(
            Position.RB, Position.LB, Position.RWB, Position.LWB,
            Position.RM, Position.LM, Position.RW, Position.LW,
            Position.CDM, Position.CAM, Position.CF
    );

    private static final Map<PitchSlotId, List<Position>> SLOT_PREFERENCE = slotPreferences();

    public LikelyLineupResponse analyze(List<PlayerSeasonResponse> squad) {
        if (!hasRolePrecision(squad)) {
            return new LikelyLineupResponse(null, false, List.of());
        }

        List<PlayerSeasonResponse> pool = squad.stream()
                .sorted(Comparator.comparingInt((PlayerSeasonResponse row) ->
                        row.minutesPlayed() == null ? 0 : row.minutesPlayed()).reversed())
                .limit(20)
                .toList();

        FormationId formation = pickFormation(pool, squad);
        List<PitchSlot> slots = FORMATIONS.get(formation);
        AssignmentResult assignment = assignFormation(slots, pool, squad);

        if (assignment.placements().size() < 11) {
            return new LikelyLineupResponse(null, true, List.of());
        }

        return new LikelyLineupResponse(formation.label, true, assignment.placements());
    }

    private static PitchSlot slot(PitchSlotId id, double x, double y) {
        return new PitchSlot(id, x, y);
    }

    private static Map<PitchSlotId, List<Position>> slotPreferences() {
        Map<PitchSlotId, List<Position>> map = new HashMap<>();
        map.put(PitchSlotId.GK, List.of(Position.GK));
        map.put(PitchSlotId.LB, List.of(Position.LB, Position.LWB, Position.LM));
        map.put(PitchSlotId.LCB, List.of(Position.CB));
        map.put(PitchSlotId.CB, List.of(Position.CB));
        map.put(PitchSlotId.RCB, List.of(Position.CB));
        map.put(PitchSlotId.RB, List.of(Position.RB, Position.RWB, Position.RM));
        map.put(PitchSlotId.LWB, List.of(Position.LWB, Position.LB, Position.LM));
        map.put(PitchSlotId.RWB, List.of(Position.RWB, Position.RB, Position.RM));
        map.put(PitchSlotId.LDM, List.of(Position.CDM, Position.CM));
        map.put(PitchSlotId.CDM, List.of(Position.CDM, Position.CM));
        map.put(PitchSlotId.RDM, List.of(Position.CDM, Position.CM));
        map.put(PitchSlotId.LCM, List.of(Position.CM, Position.CDM, Position.LM, Position.CAM));
        map.put(PitchSlotId.CM, List.of(Position.CM, Position.CDM, Position.CAM));
        map.put(PitchSlotId.RCM, List.of(Position.CM, Position.CDM, Position.RM, Position.CAM));
        map.put(PitchSlotId.LM, List.of(Position.LM, Position.LW, Position.LWB));
        map.put(PitchSlotId.RM, List.of(Position.RM, Position.RW, Position.RWB, Position.RB));
        map.put(PitchSlotId.LAM, List.of(Position.LW, Position.LM, Position.CAM));
        map.put(PitchSlotId.CAM, List.of(Position.CAM, Position.CM, Position.CF));
        map.put(PitchSlotId.RAM, List.of(Position.RW, Position.RM, Position.CAM));
        map.put(PitchSlotId.LW, List.of(Position.LW, Position.LM, Position.CF));
        map.put(PitchSlotId.ST, List.of(Position.ST, Position.CF));
        map.put(PitchSlotId.RW, List.of(Position.RW, Position.RM, Position.CF));
        return map;
    }

    static boolean hasRolePrecision(List<PlayerSeasonResponse> squad) {
        List<PlayerSeasonResponse> top = squad.stream()
                .sorted(Comparator.comparingInt((PlayerSeasonResponse row) ->
                        row.minutesPlayed() == null ? 0 : row.minutesPlayed()).reversed())
                .limit(18)
                .toList();
        int lateral = top.stream()
                .filter(row -> row.primaryPosition() != null && LATERAL.contains(row.primaryPosition()))
                .mapToInt(row -> 1)
                .sum();
        int distinct = top.stream()
                .map(PlayerSeasonResponse::primaryPosition)
                .filter(pos -> pos != null)
                .distinct()
                .mapToInt(row -> 1)
                .sum();
        return lateral >= 3 && distinct >= 5;
    }

    static SideBand sideBand(Position position) {
        if (position == null) {
            return SideBand.MID;
        }
        switch (position) {
            case GK:
                return SideBand.GK;
            case CB:
                return SideBand.CB;
            case LB, LWB, LM, LW:
                return SideBand.LEFT;
            case RB, RWB, RM, RW:
                return SideBand.RIGHT;
            case CDM, CM, CAM:
                return SideBand.MID;
            case CF, ST:
                return SideBand.ATTACK;
            default:
                return SideBand.MID;
        }
    }

    static int starterMinutesThreshold(PlayerSeasonResponse player, List<PlayerSeasonResponse> squad) {
        SideBand band = sideBand(player.primaryPosition());
        int peak = squad.stream()
                .filter(row -> sideBand(row.primaryPosition()) == band)
                .mapToInt(row -> row.minutesPlayed() == null ? 0 : row.minutesPlayed())
                .max()
                .orElse(0);
        return Math.max(STARTER_FLOOR, (int) Math.round(peak * STARTER_SHARE));
    }

    static boolean isLikelyStarter(PlayerSeasonResponse player, List<PlayerSeasonResponse> squad) {
        int minutes = player.minutesPlayed() == null ? 0 : player.minutesPlayed();
        return minutes >= starterMinutesThreshold(player, squad);
    }

    private static double scoreForSlot(
            PlayerSeasonResponse player,
            PitchSlotId slotId,
            List<PlayerSeasonResponse> squad
    ) {
        List<Position> prefs = SLOT_PREFERENCE.get(slotId);
        if (prefs == null || player.primaryPosition() == null) {
            return -1;
        }
        int rank = prefs.indexOf(player.primaryPosition());
        if (rank == -1) {
            return -1;
        }
        int minutes = player.minutesPlayed() == null ? 0 : player.minutesPlayed();
        double roleFit = 1000 - rank * 40;
        double minuteWeight = Math.min(minutes, 3600) / 3600.0;
        int threshold = starterMinutesThreshold(player, squad);
        if (minutes < threshold) {
            return roleFit * minuteWeight * 0.12;
        }
        return roleFit + minuteWeight * 600;
    }

    record AssignmentResult(List<LikelyLineupPlacementResponse> placements, int totalMinutes) {}

    private static AssignmentResult assignFormation(
            List<PitchSlot> slots,
            List<PlayerSeasonResponse> pool,
            List<PlayerSeasonResponse> squad
    ) {
        List<PlayerSeasonResponse> remaining = new ArrayList<>(pool);
        List<LikelyLineupPlacementResponse> placements = new ArrayList<>();

        for (PitchSlot slot : slots) {
            int bestIndex = -1;
            double bestScore = -1;
            for (int i = 0; i < remaining.size(); i++) {
                double score = scoreForSlot(remaining.get(i), slot.id, squad);
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                }
            }
            PlayerSeasonResponse player;
            if (bestIndex == -1) {
                if (remaining.isEmpty()) {
                    break;
                }
                player = remaining.remove(0);
            } else {
                player = remaining.remove(bestIndex);
            }
            placements.add(new LikelyLineupPlacementResponse(
                    slot.id.name(),
                    slot.x,
                    slot.y,
                    player,
                    isLikelyStarter(player, squad)
            ));
        }

        int totalMinutes = placements.stream()
                .mapToInt(row -> row.player().minutesPlayed() == null ? 0 : row.player().minutesPlayed())
                .sum();
        return new AssignmentResult(placements, totalMinutes);
    }

    private static FormationId pickFormation(List<PlayerSeasonResponse> pool, List<PlayerSeasonResponse> squad) {
        FormationId bestId = FormationId.FOUR_THREE_THREE;
        int bestMinutes = -1;
        int bestStarterCount = -1;

        for (FormationId id : FormationId.values()) {
            AssignmentResult result = assignFormation(FORMATIONS.get(id), pool, squad);
            if (result.placements().size() < 11) {
                continue;
            }
            int starterCount = result.placements().stream()
                    .filter(row -> isLikelyStarter(row.player(), squad))
                    .mapToInt(row -> 1)
                    .sum();
            if (result.totalMinutes() > bestMinutes
                    || (result.totalMinutes() == bestMinutes && starterCount > bestStarterCount)) {
                bestMinutes = result.totalMinutes();
                bestStarterCount = starterCount;
                bestId = id;
            }
        }
        return bestId;
    }
}
