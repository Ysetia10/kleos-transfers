package com.kleos.transfers.season.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API representation of a season identity record.
 */
public record SeasonResponse(
        UUID id,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        Instant createdAt,
        Instant updatedAt
) {
}
