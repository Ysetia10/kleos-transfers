package com.kleos.transfers.season.dto;

import com.kleos.transfers.common.validation.DateRangeRequest;
import com.kleos.transfers.common.validation.EndDateAfterStartDate;
import com.kleos.transfers.common.validation.SeasonLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request payload for updating a season identity.
 */
@EndDateAfterStartDate
public record UpdateSeasonRequest(
        @NotBlank @Size(min = 4, max = 20) @SeasonLabel String label,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) implements DateRangeRequest {
}
