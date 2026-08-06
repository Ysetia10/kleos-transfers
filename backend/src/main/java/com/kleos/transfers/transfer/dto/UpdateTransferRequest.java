package com.kleos.transfers.transfer.dto;

import com.kleos.transfers.common.validation.DistinctClubs;
import com.kleos.transfers.common.validation.TransferClubPair;
import com.kleos.transfers.domain.TransferType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for updating a transfer record.
 */
@DistinctClubs
public record UpdateTransferRequest(
        @NotNull UUID playerId,
        UUID fromClubId,
        UUID toClubId,
        @NotNull UUID seasonId,
        @NotNull LocalDate transferDate,
        @DecimalMin("0.0") BigDecimal feeEur,
        @NotNull TransferType type
) implements TransferClubPair {
}
