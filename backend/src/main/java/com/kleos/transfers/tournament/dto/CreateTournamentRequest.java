package com.kleos.transfers.tournament.dto;

import com.kleos.transfers.common.validation.FootballNationalityCode;
import com.kleos.transfers.domain.Confederation;
import com.kleos.transfers.domain.TournamentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a tournament identity.
 *
 * <p>{@code countryCode} is optional and used for domestic competitions only.
 * Continental/worldwide tournaments omit it.
 */
public record CreateTournamentRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Size(min = 2, max = 40) String shortName,
        @NotNull Confederation confederation,
        @NotNull TournamentType type,
        @Size(min = 3, max = 3) @FootballNationalityCode String countryCode
) {
}
