package com.kleos.transfers.contract.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API representation of a contract record.
 */
public record ContractResponse(
        UUID id,
        UUID playerId,
        String playerName,
        UUID clubId,
        String clubName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal releaseClauseEur,
        Instant createdAt,
        Instant updatedAt
) {
}
