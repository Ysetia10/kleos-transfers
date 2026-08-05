package com.kleos.transfers.mapper;

import com.kleos.transfers.dto.CreatePlayerRequest;
import com.kleos.transfers.dto.PlayerResponse;
import com.kleos.transfers.dto.UpdatePlayerRequest;
import com.kleos.transfers.entity.Player;
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
                request.primaryPosition()
        );
    }

    public void updateEntity(Player player, UpdatePlayerRequest request) {
        player.update(
                request.fullName(),
                request.dateOfBirth(),
                request.nationality(),
                request.heightCm(),
                request.preferredFoot(),
                request.primaryPosition()
        );
    }

    public PlayerResponse toResponse(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getFullName(),
                player.getDateOfBirth(),
                player.getNationality(),
                player.getHeightCm(),
                player.getPreferredFoot(),
                player.getPrimaryPosition(),
                player.getCreatedAt(),
                player.getUpdatedAt()
        );
    }
}
