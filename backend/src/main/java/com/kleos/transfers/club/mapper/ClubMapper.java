package com.kleos.transfers.club.mapper;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
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
        return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getShortName(),
                club.getCountryCode(),
                club.getFoundedYear(),
                club.getFbrefId(),
                club.getCreatedAt(),
                club.getUpdatedAt()
        );
    }
}
