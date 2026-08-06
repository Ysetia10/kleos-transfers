package com.kleos.transfers.manager.dto;

import com.kleos.transfers.common.validation.FootballNationalityCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request payload for updating a manager identity.
 */
public record UpdateManagerRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotNull @PastOrPresent LocalDate dateOfBirth,
        @NotBlank @Size(min = 3, max = 3) @FootballNationalityCode String nationality
) {
}
