package com.kleos.transfers.managerseason.dto;

import com.kleos.transfers.domain.TacticalSystem;
import com.kleos.transfers.domain.TempoProfile;
import java.math.BigDecimal;
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
        TacticalSystem tacticalSystem,
        TempoProfile tempo,
        BigDecimal youthMinutesPct,
        Instant createdAt,
        Instant updatedAt
) {
}
