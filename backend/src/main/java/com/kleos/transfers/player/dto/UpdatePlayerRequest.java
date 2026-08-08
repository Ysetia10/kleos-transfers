package com.kleos.transfers.player.dto;

import com.kleos.transfers.common.validation.FootballNationalityCode;
import com.kleos.transfers.domain.DateOfBirthPrecision;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request payload for updating a player identity.
 */
public record UpdatePlayerRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotNull @PastOrPresent LocalDate dateOfBirth,
        DateOfBirthPrecision dateOfBirthPrecision,
        @NotBlank @Size(min = 3, max = 3) @FootballNationalityCode String nationality,
        @Min(140) @Max(230) Integer heightCm,
        PreferredFoot preferredFoot,
        @NotNull Position primaryPosition,
        @Size(max = 120) String fbrefId
) {
}
