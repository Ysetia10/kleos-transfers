package com.kleos.transfers.club.mapper;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.CurrentManagerView;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.club.entity.Club;
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

    public ClubResponse toResponse(Club club) {
        return toResponse(club, null);
    }

    public ClubResponse toResponse(Club club, CurrentManagerView currentManager) {
        return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getShortName(),
                club.getCountryCode(),
                club.getFoundedYear(),
                club.getFbrefId(),
                currentManager == null ? null : currentManager.getManagerId(),
                currentManager == null ? null : currentManager.getManagerName(),
                currentManager == null ? null : currentManager.getSeasonLabel(),
                club.getCreatedAt(),
                club.getUpdatedAt()
        );
    }
}
