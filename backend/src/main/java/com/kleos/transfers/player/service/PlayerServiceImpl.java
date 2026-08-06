package com.kleos.transfers.player.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.player.dto.CreatePlayerRequest;
import com.kleos.transfers.player.dto.PlayerResponse;
import com.kleos.transfers.player.dto.UpdatePlayerRequest;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.mapper.PlayerMapper;
import com.kleos.transfers.player.repository.PlayerRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for player identity use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public PlayerResponse create(CreatePlayerRequest request) {
        Player player = playerMapper.toEntity(request);
        return playerMapper.toResponse(playerRepository.save(player));
    }

    @Override
    @Transactional
    public BulkImportResponse<PlayerResponse> createAll(List<CreatePlayerRequest> requests) {
        return bulkImporter.importAll(requests, new PlayerBulkSpec());
    }

    @Override
    public Page<PlayerResponse> findAll(Pageable pageable) {
        return playerRepository.findAll(pageable).map(playerMapper::toResponse);
    }

    @Override
    public PlayerResponse findById(UUID id) {
        return playerMapper.toResponse(findPlayer(id));
    }

    @Override
    @Transactional
    public PlayerResponse update(UUID id, UpdatePlayerRequest request) {
        Player player = findPlayer(id);
        playerMapper.updateEntity(player, request);
        return playerMapper.toResponse(player);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        Player player = findPlayer(id);
        player.softDelete();
    }

    private Player findPlayer(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Player", id));
    }

    /**
     * A player is considered a duplicate when name, date of birth, and nationality all match.
     */
    private final class PlayerBulkSpec implements BulkImportSpec<CreatePlayerRequest, PlayerResponse> {

        @Override
        public String naturalKey(CreatePlayerRequest request) {
            return NaturalKeys.of(request.fullName(), request.dateOfBirth(), request.nationality());
        }

        @Override
        public String reference(CreatePlayerRequest request) {
            return String.valueOf(request.fullName());
        }

        @Override
        public Set<String> findExistingKeys(List<CreatePlayerRequest> requests) {
            Set<String> names = requests.stream()
                    .map(request -> request.fullName().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            return playerRepository.findAllByNormalizedName(names).stream()
                    .map(player -> NaturalKeys.of(
                            player.getFullName(), player.getDateOfBirth(), player.getNationality()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<PlayerResponse> persist(List<CreatePlayerRequest> accepted) {
            List<Player> players = accepted.stream().map(playerMapper::toEntity).toList();
            return playerRepository.saveAll(players).stream().map(playerMapper::toResponse).toList();
        }
    }
}
