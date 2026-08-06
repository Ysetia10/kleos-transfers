package com.kleos.transfers.injury.dto;

import com.kleos.transfers.common.validation.DateRangeRequest;
import com.kleos.transfers.common.validation.EndDateAfterStartDate;
import com.kleos.transfers.domain.InjurySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for updating an injury spell, typically to close it with a return date.
 */
@EndDateAfterStartDate(inclusive = true, message = "endDate must not be before startDate")
public record UpdateInjuryRequest(
        @NotNull UUID playerId,
        @NotBlank @Size(max = 80) String injuryType,
        @NotNull InjurySeverity severity,
        @NotNull LocalDate startDate,
        LocalDate endDate
) implements DateRangeRequest {
}
