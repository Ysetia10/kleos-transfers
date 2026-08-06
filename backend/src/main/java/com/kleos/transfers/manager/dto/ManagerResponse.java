package com.kleos.transfers.manager.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * API representation of a manager identity record.
 */
public record ManagerResponse(
        UUID id,
        String fullName,
        LocalDate dateOfBirth,
        String nationality,
        Instant createdAt,
        Instant updatedAt
) {
}
