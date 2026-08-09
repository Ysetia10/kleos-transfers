package com.kleos.transfers.managerseason.dto;

import com.kleos.transfers.domain.TacticalSystem;
import com.kleos.transfers.domain.TempoProfile;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload for creating a manager-season appointment record.
 */
public record CreateManagerSeasonRequest(
        @NotNull UUID managerId,
        @NotNull UUID clubId,
        @NotNull UUID seasonId,
        TacticalSystem tacticalSystem,
        TempoProfile tempo,
        @DecimalMin("0") @DecimalMax("100") BigDecimal youthMinutesPct
) {
}
