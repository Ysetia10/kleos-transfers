package com.kleos.transfers.club.dto;

import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import java.util.List;

/**
 * Inferred starting XI from season minutes and enriched positions.
 */
public record LikelyLineupResponse(
        String formation,
        boolean rolePrecisionAvailable,
        List<LikelyLineupPlacementResponse> placements
) {
}
