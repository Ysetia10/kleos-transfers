package com.kleos.transfers.club.mapper;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.CurrentManagerView;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.service.ClubFitIndexCalculator;
import com.kleos.transfers.common.dto.UpdateIdentityMediaRequest;
import com.kleos.transfers.domain.TacticalSystem;
import com.kleos.transfers.domain.TempoProfile;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Maps club identity persistence models to and from API contracts.
 */
@Component
public class ClubMapper {

    public Club toEntity(CreateClubRequest request) {
        return new Club(
                request.name(),
                request.shortName(),
                request.countryCode(),
                request.foundedYear(),
                request.fbrefId()
        );
    }

    public void updateEntity(Club club, UpdateClubRequest request) {
        club.update(
                request.name(),
                request.shortName(),
                request.countryCode(),
                request.foundedYear(),
                request.fbrefId()
        );
    }

    public void updateMedia(Club club, UpdateIdentityMediaRequest request) {
        club.updateMedia(
                request.imageUrl(),
                request.attribution(),
                request.license(),
                request.source()
        );
    }

    public ClubResponse toResponse(
            Club club,
            CurrentManagerView currentManager,
            TacticalSystem tacticalSystem,
            TempoProfile tempo,
            BigDecimal youthMinutesPct,
            boolean hasSquadSeason,
            ClubFitIndexCalculator.Result fit
    ) {
        return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getShortName(),
                club.getCountryCode(),
                club.getFoundedYear(),
                club.getFbrefId(),
                club.getCrestUrl(),
                club.getCrestAttribution(),
                club.getCrestLicense(),
                club.getCrestSource(),
                currentManager == null ? null : currentManager.getManagerId(),
                currentManager == null ? null : currentManager.getManagerName(),
                currentManager == null ? null : currentManager.getSeasonLabel(),
                tacticalSystem,
                tempo,
                youthMinutesPct,
                fit.fitIndex(),
                fit.recruitmentSignal(),
                fit.version(),
                club.getCreatedAt(),
                club.getUpdatedAt()
        );
    }
}
