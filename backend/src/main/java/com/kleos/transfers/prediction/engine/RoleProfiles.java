package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.Position;
import java.util.EnumMap;
import java.util.Map;

/**
 * Exact-role competition profiles: how many starting slots a role owns in a matchday XI, and how
 * strongly a rival role contests the same minutes.
 *
 * <p>A right-back is not competing with a left-back, and a left winger is not simply a striker, so
 * competition is measured per role with partial overlap instead of one flat "forwards" bucket.
 *
 * <p>FBref season tables only publish GK/DF/MF/FW, which land as GK/CB/CM/ST. For squads still on
 * those codes a "CB" really means "a defender", so callers switch to {@link #lineSlots} plus the
 * coarse {@link PositionGroups} bucket instead of the exact matrix.
 */
final class RoleProfiles {

    private static final Map<Position, Double> EXACT_SLOTS = new EnumMap<>(Position.class);
    private static final Map<Position, Map<Position, Double>> OVERLAP = new EnumMap<>(Position.class);

    static {
        EXACT_SLOTS.put(Position.GK, 1.0);
        EXACT_SLOTS.put(Position.RB, 1.0);
        EXACT_SLOTS.put(Position.LB, 1.0);
        EXACT_SLOTS.put(Position.RWB, 1.0);
        EXACT_SLOTS.put(Position.LWB, 1.0);
        EXACT_SLOTS.put(Position.CB, 2.0);
        EXACT_SLOTS.put(Position.CDM, 1.0);
        EXACT_SLOTS.put(Position.CM, 2.0);
        EXACT_SLOTS.put(Position.CAM, 1.0);
        EXACT_SLOTS.put(Position.RM, 1.0);
        EXACT_SLOTS.put(Position.LM, 1.0);
        EXACT_SLOTS.put(Position.RW, 1.0);
        EXACT_SLOTS.put(Position.LW, 1.0);
        EXACT_SLOTS.put(Position.CF, 1.0);
        EXACT_SLOTS.put(Position.ST, 1.0);

        link(Position.RB, Position.RWB, 0.90);
        link(Position.LB, Position.LWB, 0.90);
        link(Position.RB, Position.LB, 0.15);
        link(Position.RWB, Position.LWB, 0.15);
        link(Position.RB, Position.LWB, 0.10);
        link(Position.LB, Position.RWB, 0.10);
        link(Position.RB, Position.CB, 0.30);
        link(Position.LB, Position.CB, 0.30);
        link(Position.RWB, Position.CB, 0.20);
        link(Position.LWB, Position.CB, 0.20);
        link(Position.RB, Position.RM, 0.35);
        link(Position.LB, Position.LM, 0.35);
        link(Position.RWB, Position.RM, 0.55);
        link(Position.LWB, Position.LM, 0.55);
        link(Position.RB, Position.RW, 0.20);
        link(Position.LB, Position.LW, 0.20);
        link(Position.RWB, Position.RW, 0.35);
        link(Position.LWB, Position.LW, 0.35);
        link(Position.CB, Position.CDM, 0.30);
        link(Position.CDM, Position.CM, 0.70);
        link(Position.CM, Position.CAM, 0.60);
        link(Position.CDM, Position.CAM, 0.25);
        link(Position.CM, Position.RM, 0.40);
        link(Position.CM, Position.LM, 0.40);
        link(Position.RM, Position.LM, 0.25);
        link(Position.RM, Position.RW, 0.75);
        link(Position.LM, Position.LW, 0.75);
        link(Position.RM, Position.LW, 0.20);
        link(Position.LM, Position.RW, 0.20);
        link(Position.RW, Position.LW, 0.45);
        link(Position.RW, Position.ST, 0.35);
        link(Position.LW, Position.ST, 0.35);
        link(Position.RW, Position.CF, 0.35);
        link(Position.LW, Position.CF, 0.35);
        link(Position.CAM, Position.RW, 0.45);
        link(Position.CAM, Position.LW, 0.45);
        link(Position.CAM, Position.CF, 0.50);
        link(Position.CAM, Position.ST, 0.30);
        link(Position.CF, Position.ST, 0.90);
    }

    private RoleProfiles() {
    }

    private static void link(Position left, Position right, double weight) {
        OVERLAP.computeIfAbsent(left, key -> new EnumMap<>(Position.class)).put(right, weight);
        OVERLAP.computeIfAbsent(right, key -> new EnumMap<>(Position.class)).put(left, weight);
    }

    /**
     * True when the code carries no flank/depth information because it came from a GK/DF/MF/FW feed.
     */
    static boolean isCoarse(Position position) {
        return position == Position.CB || position == Position.CM || position == Position.ST;
    }

    /**
     * Starting slots a club typically fields at this exact role.
     */
    static double exactSlots(Position position) {
        return EXACT_SLOTS.getOrDefault(position, 1.0);
    }

    /**
     * Starting slots for the whole line a coarse code stands for (defence, midfield, attack).
     */
    static double lineSlots(Position position) {
        return switch (line(position)) {
            case GOALKEEPER -> 1.0;
            case DEFENCE -> 4.0;
            case MIDFIELD -> 3.0;
            case ATTACK -> 3.0;
        };
    }

    /**
     * Share of {@code role}'s minutes a player listed at {@code rival} realistically contests.
     */
    static double exactOverlap(Position role, Position rival) {
        if (role == rival) {
            return 1.0;
        }
        return OVERLAP.getOrDefault(role, Map.of()).getOrDefault(rival, 0.0);
    }

    /**
     * Line-level overlap used while roles are still coarse: everyone in the same line contests.
     */
    static double lineOverlap(Position role, Position rival) {
        return line(role) == line(rival) ? 1.0 : 0.0;
    }

    private enum Line {
        GOALKEEPER,
        DEFENCE,
        MIDFIELD,
        ATTACK
    }

    private static Line line(Position position) {
        return switch (position) {
            case GK -> Line.GOALKEEPER;
            case RB, CB, LB, RWB, LWB -> Line.DEFENCE;
            case CDM, CM, CAM, RM, LM -> Line.MIDFIELD;
            case RW, LW, CF, ST -> Line.ATTACK;
        };
    }
}
