package com.kleos.transfers.club.dto;

import com.kleos.transfers.common.validation.FootballNationalityCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a club identity.
 */
public record UpdateClubRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Size(min = 2, max = 40) String shortName,
        @NotBlank @Size(min = 3, max = 3) @FootballNationalityCode String countryCode,
        @Min(1800) @Max(2100) Integer foundedYear,
        @Size(max = 120) String fbrefId
) {
}
