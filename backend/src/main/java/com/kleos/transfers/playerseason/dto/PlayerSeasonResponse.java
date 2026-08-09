package com.kleos.transfers.playerseason.dto;

import com.kleos.transfers.domain.Position;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a player-season performance record.
 */
public record PlayerSeasonResponse(
        UUID id,
        UUID playerId,
        String playerName,
        String photoUrl,
        UUID clubId,
        String clubName,
        UUID seasonId,
        String seasonLabel,
        Integer appearances,
        Integer minutesPlayed,
        Integer goals,
        Integer assists,
        BigDecimal xg,
        BigDecimal xa,
        Position primaryPosition,
        Instant createdAt,
        Instant updatedAt
) {
}
