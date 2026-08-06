package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.domain.Position;
import java.util.EnumSet;
import java.util.Set;

/**
 * Coarse position buckets used for squad-competition heuristics.
 */
final class PositionGroups {

    private PositionGroups() {
    }

    static Set<Position> groupOf(Position position) {
        return switch (position) {
            case GK -> EnumSet.of(Position.GK);
            case RB, LB, RWB, LWB -> EnumSet.of(Position.RB, Position.LB, Position.RWB, Position.LWB);
            case CB -> EnumSet.of(Position.CB);
            case CDM, CM, CAM, RM, LM -> EnumSet.of(
                    Position.CDM, Position.CM, Position.CAM, Position.RM, Position.LM);
            case RW, LW, CF, ST -> EnumSet.of(Position.RW, Position.LW, Position.CF, Position.ST);
        };
    }

    static boolean isAttacker(Position position) {
        return groupOf(position).contains(Position.ST) || position == Position.CAM;
    }
}
