package com.kleos.transfers.playerseason.mapper;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.playerseason.dto.CreatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.playerseason.dto.UpdatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.season.entity.Season;
import org.springframework.stereotype.Component;

/**
 * Maps player-season persistence models to and from API contracts.
 */
@Component
public class PlayerSeasonMapper {

    public PlayerSeason toEntity(
            Player player,
            Club club,
            Season season,
            CreatePlayerSeasonRequest request
    ) {
        return new PlayerSeason(
                player,
                club,
                season,
                request.appearances(),
                request.minutesPlayed(),
                request.goals(),
                request.assists(),
                request.xg(),
                request.xa(),
                request.primaryPosition()
        );
    }

    public void updateEntity(
            PlayerSeason playerSeason,
            Player player,
            Club club,
            Season season,
            UpdatePlayerSeasonRequest request
    ) {
        playerSeason.reassign(
                player,
                club,
                season,
                request.appearances(),
                request.minutesPlayed(),
                request.goals(),
                request.assists(),
                request.xg(),
                request.xa(),
                request.primaryPosition()
        );
    }

    public PlayerSeasonResponse toResponse(PlayerSeason playerSeason) {
        Player player = playerSeason.getPlayer();
        Club club = playerSeason.getClub();
        Season season = playerSeason.getSeason();
        return new PlayerSeasonResponse(
                playerSeason.getId(),
                player.getId(),
                player.getFullName(),
                club.getId(),
                club.getName(),
                season.getId(),
                season.getLabel(),
                playerSeason.getAppearances(),
                playerSeason.getMinutesPlayed(),
                playerSeason.getGoals(),
                playerSeason.getAssists(),
                playerSeason.getXg(),
                playerSeason.getXa(),
                playerSeason.getPrimaryPosition(),
                playerSeason.getCreatedAt(),
                playerSeason.getUpdatedAt()
        );
    }
}
