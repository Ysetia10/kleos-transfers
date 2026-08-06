package com.kleos.transfers.contract.service;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.contract.dto.ContractResponse;
import com.kleos.transfers.contract.dto.CreateContractRequest;
import com.kleos.transfers.contract.dto.UpdateContractRequest;
import com.kleos.transfers.contract.entity.Contract;
import com.kleos.transfers.contract.mapper.ContractMapper;
import com.kleos.transfers.contract.repository.ContractRepository;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for contract historical use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final PlayerRepository playerRepository;
    private final ClubRepository clubRepository;
    private final ContractMapper contractMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public ContractResponse create(CreateContractRequest request) {
        Player player = requirePlayer(request.playerId());
        Club club = requireClub(request.clubId());
        Contract contract = contractMapper.toEntity(player, club, request);
        return contractMapper.toResponse(contractRepository.save(contract));
    }

    @Override
    @Transactional
    public BulkImportResponse<ContractResponse> createAll(List<CreateContractRequest> requests) {
        return bulkImporter.importAll(requests, new ContractBulkSpec());
    }

    @Override
    public Page<ContractResponse> findAll(Pageable pageable) {
        return contractRepository.findAll(pageable).map(contractMapper::toResponse);
    }

    @Override
    public ContractResponse findById(UUID id) {
        return contractMapper.toResponse(findContract(id));
    }

    @Override
    @Transactional
    public ContractResponse update(UUID id, UpdateContractRequest request) {
        Contract contract = findContract(id);
        Player player = requirePlayer(request.playerId());
        Club club = requireClub(request.clubId());
        contractMapper.updateEntity(contract, player, club, request);
        return contractMapper.toResponse(contract);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findContract(id).softDelete();
    }

    private Contract findContract(UUID id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Contract", id));
    }

    private Player requirePlayer(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Player", id));
    }

    private Club requireClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
    }

    private final class ContractBulkSpec implements BulkImportSpec<CreateContractRequest, ContractResponse> {

        @Override
        public String naturalKey(CreateContractRequest request) {
            return NaturalKeys.of(request.playerId(), request.clubId(), request.startDate());
        }

        @Override
        public String reference(CreateContractRequest request) {
            return request.playerId() + " @ " + request.clubId();
        }

        @Override
        public Set<String> findExistingKeys(List<CreateContractRequest> requests) {
            Set<String> keys = requests.stream()
                    .map(request -> request.playerId() + ":" + request.clubId() + ":" + request.startDate())
                    .collect(Collectors.toSet());
            return contractRepository.findAllByUniquenessKeyIn(keys).stream()
                    .map(contract -> NaturalKeys.of(
                            contract.getPlayer().getId(),
                            contract.getClub().getId(),
                            contract.getStartDate()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<ContractResponse> persist(List<CreateContractRequest> accepted) {
            Set<UUID> playerIds = new HashSet<>();
            Set<UUID> clubIds = new HashSet<>();
            for (CreateContractRequest request : accepted) {
                playerIds.add(request.playerId());
                clubIds.add(request.clubId());
            }

            Map<UUID, Player> players = playerRepository.findAllById(playerIds).stream()
                    .collect(Collectors.toMap(Player::getId, player -> player));
            Map<UUID, Club> clubs = clubRepository.findAllById(clubIds).stream()
                    .collect(Collectors.toMap(Club::getId, club -> club));

            List<Contract> entities = accepted.stream()
                    .map(request -> contractMapper.toEntity(
                            requirePresent(players, request.playerId(), "Player"),
                            requirePresent(clubs, request.clubId(), "Club"),
                            request))
                    .toList();

            return contractRepository.saveAll(entities).stream()
                    .map(contractMapper::toResponse)
                    .toList();
        }

        private <T> T requirePresent(Map<UUID, T> indexed, UUID id, String resource) {
            T value = indexed.get(id);
            if (value == null) {
                throw ResourceNotFoundException.of(resource, id);
            }
            return value;
        }
    }
}
