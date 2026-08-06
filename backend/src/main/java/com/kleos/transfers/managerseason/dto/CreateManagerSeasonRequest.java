package com.kleos.transfers.managerseason.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload for creating a manager-season appointment record.
 */
public record CreateManagerSeasonRequest(
        @NotNull UUID managerId,
        @NotNull UUID clubId,
        @NotNull UUID seasonId
) {
}
