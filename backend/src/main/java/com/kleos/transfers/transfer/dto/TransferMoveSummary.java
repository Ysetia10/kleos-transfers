package com.kleos.transfers.transfer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Compact transfer fact for badges/tooltips (from → to + optional fee).
 */
public record TransferMoveSummary(
        UUID transferId,
        UUID fromClubId,
        String fromClubName,
        UUID toClubId,
        String toClubName,
        BigDecimal feeEur,
        LocalDate transferDate,
        String seasonLabel,
        LocalDate seasonStartDate
) {
}
