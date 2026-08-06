package com.kleos.transfers.contract.dto;

import com.kleos.transfers.common.validation.DateRangeRequest;
import com.kleos.transfers.common.validation.EndDateAfterStartDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for updating a contract record.
 */
@EndDateAfterStartDate
public record UpdateContractRequest(
        @NotNull UUID playerId,
        @NotNull UUID clubId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @DecimalMin("0.0") BigDecimal releaseClauseEur
) implements DateRangeRequest {
}
