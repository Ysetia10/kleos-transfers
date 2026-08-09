package com.kleos.transfers.transfer.dto;

import com.kleos.transfers.domain.TransferStatus;
import com.kleos.transfers.domain.TransferType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API representation of a transfer record.
 */
public record TransferResponse(
        UUID id,
        UUID playerId,
        String playerName,
        UUID fromClubId,
        String fromClubName,
        UUID toClubId,
        String toClubName,
        UUID seasonId,
        String seasonLabel,
        LocalDate transferDate,
        BigDecimal feeEur,
        TransferType type,
        TransferStatus status,
        String source,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
