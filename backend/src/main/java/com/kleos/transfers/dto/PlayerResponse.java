package com.kleos.transfers.dto;

import com.kleos.transfers.entity.enums.Position;
import com.kleos.transfers.entity.enums.PreferredFoot;
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
        String nationality,
        Integer heightCm,
        PreferredFoot preferredFoot,
        Position primaryPosition,
        Instant createdAt,
        Instant updatedAt
) {
}
