package com.kleos.transfers.player.dto;

import com.kleos.transfers.domain.DateOfBirthPrecision;
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
        DateOfBirthPrecision dateOfBirthPrecision,
        Integer age,
        String nationality,
        Integer heightCm,
        PreferredFoot preferredFoot,
        Position primaryPosition,
        String fbrefId,
        String photoUrl,
        String photoAttribution,
        String photoLicense,
        String photoSource,
        UUID latestClubId,
        String latestClubName,
        String latestSeasonLabel,
        Instant createdAt,
        Instant updatedAt
) {
}
