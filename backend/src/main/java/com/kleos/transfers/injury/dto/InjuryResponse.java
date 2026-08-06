package com.kleos.transfers.injury.dto;

import com.kleos.transfers.domain.InjurySeverity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API representation of an injury spell.
 *
 * <p>{@code daysOut} is derived from the date range and is null while ongoing.
 */
public record InjuryResponse(
        UUID id,
        UUID playerId,
        String playerName,
        String injuryType,
        InjurySeverity severity,
        LocalDate startDate,
        LocalDate endDate,
        Integer daysOut,
        boolean ongoing,
        Instant createdAt,
        Instant updatedAt
) {
}
