package com.kleos.transfers.playerseason.mapper;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.playerseason.dto.CreatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.playerseason.dto.UpdatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.season.entity.Season;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
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
                player.getPhotoUrl(),
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

    /**
     * Retains prior-season stats but labels the row for the projected destination season/club.
     */
    public PlayerSeasonResponse toProjectedResponse(PlayerSeason playerSeason, Club club, Season season) {
        PlayerSeasonResponse base = toResponse(playerSeason);
        return new PlayerSeasonResponse(
                base.id(),
                base.playerId(),
                base.playerName(),
                base.photoUrl(),
                club.getId(),
                club.getName(),
                season.getId(),
                season.getLabel(),
                base.appearances(),
                base.minutesPlayed(),
                base.goals(),
                base.assists(),
                base.xg(),
                base.xa(),
                base.primaryPosition(),
                base.createdAt(),
                base.updatedAt()
        );
    }

    /**
     * Arrival without a destination PlayerSeason yet — stats from the player's latest prior row when present.
     */
    public PlayerSeasonResponse toProjectedArrival(
            Player player,
            Club club,
            Season season,
            PlayerSeason priorStats
    ) {
        if (priorStats != null) {
            return toProjectedResponse(priorStats, club, season);
        }
        Instant stamp = player.getCreatedAt() != null ? player.getCreatedAt() : Instant.EPOCH;
        Instant updated = player.getUpdatedAt() != null ? player.getUpdatedAt() : stamp;
        return new PlayerSeasonResponse(
                // Stable synthetic id for clients that key rows by PlayerSeason id.
                UUID.nameUUIDFromBytes(
                        ("projected:" + season.getId() + ":" + player.getId())
                                .getBytes(StandardCharsets.UTF_8)),
                player.getId(),
                player.getFullName(),
                player.getPhotoUrl(),
                club.getId(),
                club.getName(),
                season.getId(),
                season.getLabel(),
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                player.getPrimaryPosition(),
                stamp,
                updated
        );
    }
}
