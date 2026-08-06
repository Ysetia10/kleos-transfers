package com.kleos.transfers.club.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a club identity record.
 */
public record ClubResponse(
        UUID id,
        String name,
        String shortName,
        String countryCode,
        Integer foundedYear,
        String fbrefId,
        Instant createdAt,
        Instant updatedAt
) {
}
