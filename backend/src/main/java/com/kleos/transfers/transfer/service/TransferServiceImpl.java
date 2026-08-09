package com.kleos.transfers.transfer.service;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.domain.TransferStatus;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.transfer.dto.CreateTransferRequest;
import com.kleos.transfers.transfer.dto.TransferResponse;
import com.kleos.transfers.transfer.dto.UpdateTransferRequest;
import com.kleos.transfers.transfer.entity.Transfer;
import com.kleos.transfers.transfer.mapper.TransferMapper;
import com.kleos.transfers.transfer.repository.TransferRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for transfer historical use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final PlayerRepository playerRepository;
    private final ClubRepository clubRepository;
    private final SeasonRepository seasonRepository;
    private final TransferMapper transferMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public TransferResponse create(CreateTransferRequest request) {
        Player player = requirePlayer(request.playerId());
        Club fromClub = optionalClub(request.fromClubId());
        Club toClub = optionalClub(request.toClubId());
        Season season = requireSeason(request.seasonId());
        Transfer transfer = transferMapper.toEntity(player, fromClub, toClub, season, request);
        return transferMapper.toResponse(transferRepository.save(transfer));
    }

    @Override
    @Transactional
    public BulkImportResponse<TransferResponse> createAll(List<CreateTransferRequest> requests) {
        return bulkImporter.importAll(requests, new TransferBulkSpec());
    }

    @Override
    public Page<TransferResponse> findAll(TransferStatus status, UUID seasonId, Pageable pageable) {
        if (status != null && seasonId != null) {
            return transferRepository.findByStatusAndSeason_Id(status, seasonId, pageable)
                    .map(transferMapper::toResponse);
        }
        if (seasonId != null) {
            return transferRepository.findBySeason_Id(seasonId, pageable).map(transferMapper::toResponse);
        }
        if (status == null) {
            return transferRepository.findAll(pageable).map(transferMapper::toResponse);
        }
        return transferRepository.findByStatus(status, pageable).map(transferMapper::toResponse);
    }

    @Override
    public TransferResponse findById(UUID id) {
        return transferMapper.toResponse(findTransfer(id));
    }

    @Override
    @Transactional
    public TransferResponse update(UUID id, UpdateTransferRequest request) {
        Transfer transfer = findTransfer(id);
        Player player = requirePlayer(request.playerId());
        Club fromClub = optionalClub(request.fromClubId());
        Club toClub = optionalClub(request.toClubId());
        Season season = requireSeason(request.seasonId());
        transferMapper.updateEntity(transfer, player, fromClub, toClub, season, request);
        return transferMapper.toResponse(transfer);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findTransfer(id).softDelete();
    }

    private Transfer findTransfer(UUID id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transfer", id));
    }

    private Player requirePlayer(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Player", id));
    }

    private Season requireSeason(UUID id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Season", id));
    }

    private Club optionalClub(UUID id) {
        if (id == null) {
            return null;
        }
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
    }

    private final class TransferBulkSpec implements BulkImportSpec<CreateTransferRequest, TransferResponse> {

        @Override
        public String naturalKey(CreateTransferRequest request) {
            return NaturalKeys.of(
                    request.playerId(),
                    request.transferDate(),
                    request.fromClubId(),
                    request.toClubId(),
                    request.type(),
                    request.status() == null ? TransferStatus.COMPLETED : request.status()
            );
        }

        @Override
        public String reference(CreateTransferRequest request) {
            return request.playerId() + " @ " + request.transferDate();
        }

        @Override
        public Set<String> findExistingKeys(List<CreateTransferRequest> requests) {
            Set<String> keys = requests.stream()
                    .map(TransferServiceImpl.this::activeUniquenessKey)
                    .collect(Collectors.toSet());
            return transferRepository.findAllByUniquenessKeyIn(keys).stream()
                    .map(transfer -> NaturalKeys.of(
                            transfer.getPlayer().getId(),
                            transfer.getTransferDate(),
                            transfer.getFromClub() == null ? null : transfer.getFromClub().getId(),
                            transfer.getToClub() == null ? null : transfer.getToClub().getId(),
                            transfer.getType(),
                            transfer.getStatus()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<TransferResponse> persist(List<CreateTransferRequest> accepted) {
            Set<UUID> playerIds = new HashSet<>();
            Set<UUID> clubIds = new HashSet<>();
            Set<UUID> seasonIds = new HashSet<>();
            for (CreateTransferRequest request : accepted) {
                playerIds.add(request.playerId());
                if (request.fromClubId() != null) {
                    clubIds.add(request.fromClubId());
                }
                if (request.toClubId() != null) {
                    clubIds.add(request.toClubId());
                }
                seasonIds.add(request.seasonId());
            }

            Map<UUID, Player> players = playerRepository.findAllById(playerIds).stream()
                    .collect(Collectors.toMap(Player::getId, player -> player));
            Map<UUID, Club> clubs = clubRepository.findAllById(clubIds).stream()
                    .collect(Collectors.toMap(Club::getId, club -> club));
            Map<UUID, Season> seasons = seasonRepository.findAllById(seasonIds).stream()
                    .collect(Collectors.toMap(Season::getId, season -> season));

            List<Transfer> entities = accepted.stream()
                    .map(request -> transferMapper.toEntity(
                            requirePresent(players, request.playerId(), "Player"),
                            optionalPresent(clubs, request.fromClubId(), "Club"),
                            optionalPresent(clubs, request.toClubId(), "Club"),
                            requirePresent(seasons, request.seasonId(), "Season"),
                            request))
                    .toList();

            return transferRepository.saveAll(entities).stream()
                    .map(transferMapper::toResponse)
                    .toList();
        }

        private <T> T requirePresent(Map<UUID, T> indexed, UUID id, String resource) {
            T value = indexed.get(id);
            if (value == null) {
                throw ResourceNotFoundException.of(resource, id);
            }
            return value;
        }

        private Club optionalPresent(Map<UUID, Club> indexed, UUID id, String resource) {
            if (id == null) {
                return null;
            }
            return requirePresent(indexed, id, resource);
        }
    }

    private String activeUniquenessKey(CreateTransferRequest request) {
        TransferStatus status = request.status() == null ? TransferStatus.COMPLETED : request.status();
        return request.playerId()
                + ":"
                + request.transferDate()
                + ":"
                + Objects.toString(request.fromClubId(), "none")
                + ":"
                + Objects.toString(request.toClubId(), "none")
                + ":"
                + request.type().name()
                + ":"
                + status.name();
    }
}
