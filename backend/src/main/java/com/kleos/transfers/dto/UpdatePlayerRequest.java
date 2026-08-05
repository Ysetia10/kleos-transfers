package com.kleos.transfers.dto;

import com.kleos.transfers.entity.enums.Position;
import com.kleos.transfers.entity.enums.PreferredFoot;
import com.kleos.transfers.validation.IsoCountryCode;
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
        @NotBlank @IsoCountryCode String nationality,
        @NotNull @Min(140) @Max(230) Integer heightCm,
        @NotNull PreferredFoot preferredFoot,
        @NotNull Position primaryPosition
) {
}
