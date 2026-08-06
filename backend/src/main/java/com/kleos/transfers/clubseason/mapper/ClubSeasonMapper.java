package com.kleos.transfers.clubseason.mapper;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.clubseason.dto.ClubSeasonResponse;
import com.kleos.transfers.clubseason.entity.ClubSeason;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.tournament.entity.Tournament;
import org.springframework.stereotype.Component;

/**
 * Maps club-season persistence models to and from API contracts.
 */
@Component
public class ClubSeasonMapper {

    public ClubSeason toEntity(Club club, Season season, Tournament tournament) {
        return new ClubSeason(club, season, tournament);
    }

    public ClubSeasonResponse toResponse(ClubSeason clubSeason) {
        Club club = clubSeason.getClub();
        Season season = clubSeason.getSeason();
        Tournament tournament = clubSeason.getTournament();
        return new ClubSeasonResponse(
                clubSeason.getId(),
                club.getId(),
                club.getName(),
                season.getId(),
                season.getLabel(),
                tournament.getId(),
                tournament.getName(),
                clubSeason.getCreatedAt(),
                clubSeason.getUpdatedAt()
        );
    }
}
