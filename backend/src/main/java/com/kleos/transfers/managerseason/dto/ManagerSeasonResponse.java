package com.kleos.transfers.managerseason.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a manager-season appointment record.
 */
public record ManagerSeasonResponse(
        UUID id,
        UUID managerId,
        String managerName,
        UUID clubId,
        String clubName,
        UUID seasonId,
        String seasonLabel,
        Instant createdAt,
        Instant updatedAt
) {
}
