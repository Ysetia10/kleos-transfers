package com.kleos.transfers.club.dto;

import com.kleos.transfers.domain.RecruitmentSignal;
import com.kleos.transfers.domain.TacticalSystem;
import com.kleos.transfers.domain.TempoProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a club identity record plus absolute fit / recruitment signals.
 */
public record ClubResponse(
        UUID id,
        String name,
        String shortName,
        String countryCode,
        Integer foundedYear,
        String fbrefId,
        String crestUrl,
        String crestAttribution,
        String crestLicense,
        String crestSource,
        UUID currentManagerId,
        String currentManagerName,
        String currentManagerSeasonLabel,
        Boolean currentManagerFirstSeasonAtClub,
        TacticalSystem tacticalSystem,
        TempoProfile tempo,
        BigDecimal youthMinutesPct,
        BigDecimal fitIndex,
        RecruitmentSignal recruitmentSignal,
        String fitIndexVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
