package com.kleos.transfers.playerseason.service;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import com.kleos.transfers.playerseason.dto.CreatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.playerseason.dto.UpdatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.playerseason.mapper.PlayerSeasonMapper;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
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
 * Application service for player-season performance use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerSeasonServiceImpl implements PlayerSeasonService {

    private final PlayerSeasonRepository playerSeasonRepository;
    private final PlayerRepository playerRepository;
    private final ClubRepository clubRepository;
    private final SeasonRepository seasonRepository;
    private final PlayerSeasonMapper playerSeasonMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public PlayerSeasonResponse create(CreatePlayerSeasonRequest request) {
        Player player = requirePlayer(request.playerId());
        Club club = requireClub(request.clubId());
        Season season = requireSeason(request.seasonId());
        PlayerSeason playerSeason = playerSeasonMapper.toEntity(player, club, season, request);
        return playerSeasonMapper.toResponse(playerSeasonRepository.save(playerSeason));
    }

    @Override
    @Transactional
    public BulkImportResponse<PlayerSeasonResponse> createAll(List<CreatePlayerSeasonRequest> requests) {
        return bulkImporter.importAll(requests, new PlayerSeasonBulkSpec());
    }

    @Override
    public Page<PlayerSeasonResponse> findAll(Pageable pageable) {
        return playerSeasonRepository.findAll(pageable).map(playerSeasonMapper::toResponse);
    }

    @Override
    public PlayerSeasonResponse findById(UUID id) {
        return playerSeasonMapper.toResponse(findPlayerSeason(id));
    }

    @Override
    @Transactional
    public PlayerSeasonResponse update(UUID id, UpdatePlayerSeasonRequest request) {
        PlayerSeason playerSeason = findPlayerSeason(id);
        Player player = requirePlayer(request.playerId());
        Club club = requireClub(request.clubId());
        Season season = requireSeason(request.seasonId());
        playerSeasonMapper.updateEntity(playerSeason, player, club, season, request);
        return playerSeasonMapper.toResponse(playerSeason);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findPlayerSeason(id).softDelete();
    }

    private PlayerSeason findPlayerSeason(UUID id) {
        return playerSeasonRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("PlayerSeason", id));
    }

    private Player requirePlayer(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Player", id));
    }

    private Club requireClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
    }

    private Season requireSeason(UUID id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Season", id));
    }

    private final class PlayerSeasonBulkSpec
            implements BulkImportSpec<CreatePlayerSeasonRequest, PlayerSeasonResponse> {

        @Override
        public String naturalKey(CreatePlayerSeasonRequest request) {
            return NaturalKeys.of(request.playerId(), request.clubId(), request.seasonId());
        }

        @Override
        public String reference(CreatePlayerSeasonRequest request) {
            return request.playerId() + " / " + request.clubId() + " / " + request.seasonId();
        }

        @Override
        public Set<String> findExistingKeys(List<CreatePlayerSeasonRequest> requests) {
            Set<String> keys = requests.stream()
                    .map(request -> request.playerId() + ":" + request.clubId() + ":" + request.seasonId())
                    .collect(Collectors.toSet());
            return playerSeasonRepository.findAllByUniquenessKeyIn(keys).stream()
                    .map(playerSeason -> NaturalKeys.of(
                            playerSeason.getPlayer().getId(),
                            playerSeason.getClub().getId(),
                            playerSeason.getSeason().getId()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<PlayerSeasonResponse> persist(List<CreatePlayerSeasonRequest> accepted) {
            Set<UUID> playerIds = new HashSet<>();
            Set<UUID> clubIds = new HashSet<>();
            Set<UUID> seasonIds = new HashSet<>();
            for (CreatePlayerSeasonRequest request : accepted) {
                playerIds.add(request.playerId());
                clubIds.add(request.clubId());
                seasonIds.add(request.seasonId());
            }

            Map<UUID, Player> players = playerRepository.findAllById(playerIds).stream()
                    .collect(Collectors.toMap(Player::getId, player -> player));
            Map<UUID, Club> clubs = clubRepository.findAllById(clubIds).stream()
                    .collect(Collectors.toMap(Club::getId, club -> club));
            Map<UUID, Season> seasons = seasonRepository.findAllById(seasonIds).stream()
                    .collect(Collectors.toMap(Season::getId, season -> season));

            List<PlayerSeason> entities = accepted.stream()
                    .map(request -> playerSeasonMapper.toEntity(
                            requirePresent(players, request.playerId(), "Player"),
                            requirePresent(clubs, request.clubId(), "Club"),
                            requirePresent(seasons, request.seasonId(), "Season"),
                            request))
                    .toList();

            return playerSeasonRepository.saveAll(entities).stream()
                    .map(playerSeasonMapper::toResponse)
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
