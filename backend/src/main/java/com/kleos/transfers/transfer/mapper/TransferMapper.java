package com.kleos.transfers.transfer.mapper;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.transfer.dto.CreateTransferRequest;
import com.kleos.transfers.transfer.dto.TransferResponse;
import com.kleos.transfers.transfer.dto.UpdateTransferRequest;
import com.kleos.transfers.transfer.entity.Transfer;
import org.springframework.stereotype.Component;

/**
 * Maps transfer persistence models to and from API contracts.
 */
@Component
public class TransferMapper {

    public Transfer toEntity(
            Player player,
            Club fromClub,
            Club toClub,
            Season season,
            CreateTransferRequest request
    ) {
        return new Transfer(
                player,
                fromClub,
                toClub,
                season,
                request.transferDate(),
                request.feeEur(),
                request.type()
        );
    }

    public void updateEntity(
            Transfer transfer,
            Player player,
            Club fromClub,
            Club toClub,
            Season season,
            UpdateTransferRequest request
    ) {
        transfer.reassign(
                player,
                fromClub,
                toClub,
                season,
                request.transferDate(),
                request.feeEur(),
                request.type()
        );
    }

    public TransferResponse toResponse(Transfer transfer) {
        Player player = transfer.getPlayer();
        Club fromClub = transfer.getFromClub();
        Club toClub = transfer.getToClub();
        Season season = transfer.getSeason();
        return new TransferResponse(
                transfer.getId(),
                player.getId(),
                player.getFullName(),
                fromClub == null ? null : fromClub.getId(),
                fromClub == null ? null : fromClub.getName(),
                toClub == null ? null : toClub.getId(),
                toClub == null ? null : toClub.getName(),
                season.getId(),
                season.getLabel(),
                transfer.getTransferDate(),
                transfer.getFeeEur(),
                transfer.getType(),
                transfer.getCreatedAt(),
                transfer.getUpdatedAt()
        );
    }
}
