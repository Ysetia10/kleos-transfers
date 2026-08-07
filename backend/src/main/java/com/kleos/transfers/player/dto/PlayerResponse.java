package com.kleos.transfers.player.dto;

import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API representation of a player identity record.
 */
public record PlayerResponse(
        UUID id,
        String fullName,
        LocalDate dateOfBirth,
        Integer age,
        String nationality,
        Integer heightCm,
        PreferredFoot preferredFoot,
        Position primaryPosition,
        String fbrefId,
        UUID latestClubId,
        String latestClubName,
        String latestSeasonLabel,
        Instant createdAt,
        Instant updatedAt
) {
}
