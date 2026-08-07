package com.kleos.transfers.player.mapper;

import com.kleos.transfers.player.dto.CreatePlayerRequest;
import com.kleos.transfers.player.dto.LatestClubView;
import com.kleos.transfers.player.dto.PlayerResponse;
import com.kleos.transfers.player.dto.UpdatePlayerRequest;
import com.kleos.transfers.player.entity.Player;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.stereotype.Component;

/**
 * Maps player identity persistence models to and from API contracts.
 */
@Component
public class PlayerMapper {

    public Player toEntity(CreatePlayerRequest request) {
        return new Player(
                request.fullName(),
                request.dateOfBirth(),
                request.nationality(),
                request.heightCm(),
                request.preferredFoot(),
                request.primaryPosition(),
                request.fbrefId()
        );
    }

    public void updateEntity(Player player, UpdatePlayerRequest request) {
        player.update(
                request.fullName(),
                request.dateOfBirth(),
                request.nationality(),
                request.heightCm(),
                request.preferredFoot(),
                request.primaryPosition(),
                request.fbrefId()
        );
    }

    public PlayerResponse toResponse(Player player) {
        return toResponse(player, null);
    }

    public PlayerResponse toResponse(Player player, LatestClubView latestClub) {
        return new PlayerResponse(
                player.getId(),
                player.getFullName(),
                player.getDateOfBirth(),
                ageYears(player.getDateOfBirth()),
                player.getNationality(),
                player.getHeightCm(),
                player.getPreferredFoot(),
                player.getPrimaryPosition(),
                player.getFbrefId(),
                latestClub == null ? null : latestClub.getClubId(),
                latestClub == null ? null : latestClub.getClubName(),
                latestClub == null ? null : latestClub.getSeasonLabel(),
                player.getCreatedAt(),
                player.getUpdatedAt()
        );
    }

    private static Integer ageYears(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
