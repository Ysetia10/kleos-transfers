package com.kleos.transfers.player.dto;

import com.kleos.transfers.common.validation.FootballNationalityCode;
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
 * Request payload for creating a player identity.
 *
 * <p>{@code heightCm} and {@code preferredFoot} are optional because season-stat
 * sources often omit them. {@code fbrefId} is the preferred ingest match key.
 */
public record CreatePlayerRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotNull @PastOrPresent LocalDate dateOfBirth,
        @NotBlank @Size(min = 3, max = 3) @FootballNationalityCode String nationality,
        @Min(140) @Max(230) Integer heightCm,
        PreferredFoot preferredFoot,
        @NotNull Position primaryPosition,
        @Size(max = 40) String fbrefId
) {
}
