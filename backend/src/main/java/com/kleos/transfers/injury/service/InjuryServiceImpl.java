package com.kleos.transfers.injury.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.injury.dto.CreateInjuryRequest;
import com.kleos.transfers.injury.dto.InjuryResponse;
import com.kleos.transfers.injury.dto.UpdateInjuryRequest;
import com.kleos.transfers.injury.entity.Injury;
import com.kleos.transfers.injury.mapper.InjuryMapper;
import com.kleos.transfers.injury.repository.InjuryRepository;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import java.util.List;
import java.util.Locale;
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
 * Application service for injury historical use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InjuryServiceImpl implements InjuryService {

    private final InjuryRepository injuryRepository;
    private final PlayerRepository playerRepository;
    private final InjuryMapper injuryMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public InjuryResponse create(CreateInjuryRequest request) {
        Player player = requirePlayer(request.playerId());
        Injury injury = injuryMapper.toEntity(player, request);
        return injuryMapper.toResponse(injuryRepository.save(injury));
    }

    @Override
    @Transactional
    public BulkImportResponse<InjuryResponse> createAll(List<CreateInjuryRequest> requests) {
        return bulkImporter.importAll(requests, new InjuryBulkSpec());
    }

    @Override
    public Page<InjuryResponse> findAll(Pageable pageable) {
        return injuryRepository.findAll(pageable).map(injuryMapper::toResponse);
    }

    @Override
    public Page<InjuryResponse> findByPlayerId(UUID playerId, Pageable pageable) {
        requirePlayer(playerId);
        return injuryRepository.findByPlayer_Id(playerId, pageable).map(injuryMapper::toResponse);
    }

    @Override
    public InjuryResponse findById(UUID id) {
        return injuryMapper.toResponse(findInjury(id));
    }

    @Override
    @Transactional
    public InjuryResponse update(UUID id, UpdateInjuryRequest request) {
        Injury injury = findInjury(id);
        Player player = requirePlayer(request.playerId());
        injuryMapper.updateEntity(injury, player, request);
        return injuryMapper.toResponse(injury);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findInjury(id).softDelete();
    }

    private Injury findInjury(UUID id) {
        return injuryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Injury", id));
    }

    private Player requirePlayer(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Player", id));
    }

    private final class InjuryBulkSpec implements BulkImportSpec<CreateInjuryRequest, InjuryResponse> {

        @Override
        public String naturalKey(CreateInjuryRequest request) {
            return NaturalKeys.of(request.playerId(), request.startDate(), request.injuryType());
        }

        @Override
        public String reference(CreateInjuryRequest request) {
            return request.playerId() + " @ " + request.startDate();
        }

        @Override
        public Set<String> findExistingKeys(List<CreateInjuryRequest> requests) {
            Set<String> keys = requests.stream()
                    .map(request -> request.playerId()
                            + ":"
                            + request.startDate()
                            + ":"
                            + request.injuryType().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            return injuryRepository.findAllByUniquenessKeyIn(keys).stream()
                    .map(injury -> NaturalKeys.of(
                            injury.getPlayer().getId(),
                            injury.getStartDate(),
                            injury.getInjuryType()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<InjuryResponse> persist(List<CreateInjuryRequest> accepted) {
            Set<UUID> playerIds = accepted.stream()
                    .map(CreateInjuryRequest::playerId)
                    .collect(Collectors.toSet());
            Map<UUID, Player> players = playerRepository.findAllById(playerIds).stream()
                    .collect(Collectors.toMap(Player::getId, player -> player));

            List<Injury> entities = accepted.stream()
                    .map(request -> injuryMapper.toEntity(
                            requirePresent(players, request.playerId()),
                            request))
                    .toList();

            return injuryRepository.saveAll(entities).stream()
                    .map(injuryMapper::toResponse)
                    .toList();
        }

        private Player requirePresent(Map<UUID, Player> indexed, UUID id) {
            Player player = indexed.get(id);
            if (player == null) {
                throw ResourceNotFoundException.of("Player", id);
            }
            return player;
        }
    }
}
