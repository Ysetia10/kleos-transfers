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
        UUID currentManagerId,
        String currentManagerName,
        String currentManagerSeasonLabel,
        Instant createdAt,
        Instant updatedAt
) {
}
