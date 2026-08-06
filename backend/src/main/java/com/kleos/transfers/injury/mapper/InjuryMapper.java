package com.kleos.transfers.injury.mapper;

import com.kleos.transfers.injury.dto.CreateInjuryRequest;
import com.kleos.transfers.injury.dto.InjuryResponse;
import com.kleos.transfers.injury.dto.UpdateInjuryRequest;
import com.kleos.transfers.injury.entity.Injury;
import com.kleos.transfers.player.entity.Player;
import org.springframework.stereotype.Component;

/**
 * Maps injury persistence models to and from API contracts.
 */
@Component
public class InjuryMapper {

    public Injury toEntity(Player player, CreateInjuryRequest request) {
        return new Injury(
                player,
                request.injuryType(),
                request.severity(),
                request.startDate(),
                request.endDate()
        );
    }

    public void updateEntity(Injury injury, Player player, UpdateInjuryRequest request) {
        injury.reassign(
                player,
                request.injuryType(),
                request.severity(),
                request.startDate(),
                request.endDate()
        );
    }

    public InjuryResponse toResponse(Injury injury) {
        Player player = injury.getPlayer();
        return new InjuryResponse(
                injury.getId(),
                player.getId(),
                player.getFullName(),
                injury.getInjuryType(),
                injury.getSeverity(),
                injury.getStartDate(),
                injury.getEndDate(),
                injury.getDaysOut(),
                injury.isOngoing(),
                injury.getCreatedAt(),
                injury.getUpdatedAt()
        );
    }
}
