package com.kleos.transfers.managerseason.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request payload for updating a manager-season appointment record.
 */
public record UpdateManagerSeasonRequest(
        @NotNull UUID managerId,
        @NotNull UUID clubId,
        @NotNull UUID seasonId
) {
}
