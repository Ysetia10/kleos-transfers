package com.kleos.transfers.tournament.dto;

import com.kleos.transfers.domain.Confederation;
import com.kleos.transfers.domain.TournamentType;
import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a tournament identity record.
 */
public record TournamentResponse(
        UUID id,
        String name,
        String shortName,
        Confederation confederation,
        TournamentType type,
        String countryCode,
        Instant createdAt,
        Instant updatedAt
) {
}
