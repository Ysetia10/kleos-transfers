package com.kleos.transfers.club.dto;

import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;

/**
 * One player placed on a pitch slot in a likely XI.
 */
public record LikelyLineupPlacementResponse(
        String slotId,
        double x,
        double y,
        PlayerSeasonResponse player,
        boolean likelyStarter
) {
}
