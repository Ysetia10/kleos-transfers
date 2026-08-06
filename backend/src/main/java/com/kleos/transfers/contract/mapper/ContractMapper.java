package com.kleos.transfers.contract.mapper;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.contract.dto.ContractResponse;
import com.kleos.transfers.contract.dto.CreateContractRequest;
import com.kleos.transfers.contract.dto.UpdateContractRequest;
import com.kleos.transfers.contract.entity.Contract;
import com.kleos.transfers.player.entity.Player;
import org.springframework.stereotype.Component;

/**
 * Maps contract persistence models to and from API contracts.
 */
@Component
public class ContractMapper {

    public Contract toEntity(Player player, Club club, CreateContractRequest request) {
        return new Contract(
                player,
                club,
                request.startDate(),
                request.endDate(),
                request.releaseClauseEur()
        );
    }

    public void updateEntity(Contract contract, Player player, Club club, UpdateContractRequest request) {
        contract.reassign(
                player,
                club,
                request.startDate(),
                request.endDate(),
                request.releaseClauseEur()
        );
    }

    public ContractResponse toResponse(Contract contract) {
        Player player = contract.getPlayer();
        Club club = contract.getClub();
        return new ContractResponse(
                contract.getId(),
                player.getId(),
                player.getFullName(),
                club.getId(),
                club.getName(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getReleaseClauseEur(),
                contract.getCreatedAt(),
                contract.getUpdatedAt()
        );
    }
}
