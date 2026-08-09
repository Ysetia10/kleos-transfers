package com.kleos.transfers.player.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ConflictException;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.common.dto.UpdateIdentityMediaRequest;
import com.kleos.transfers.common.search.SearchQueries;
import com.kleos.transfers.domain.FootballCountryNames;
import com.kleos.transfers.player.dto.CreatePlayerRequest;
import com.kleos.transfers.player.dto.LatestClubView;
import com.kleos.transfers.player.dto.PlayerResponse;
import com.kleos.transfers.player.dto.UpdatePlayerRequest;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.mapper.PlayerMapper;
import com.kleos.transfers.player.repository.PlayerRepository;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final PlayerSeasonRepository playerSeasonRepository;
    private final PlayerMapper playerMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public PlayerResponse create(CreatePlayerRequest request) {
        assertUnique(request.fullName(), request.dateOfBirth(), request.nationality(), request.fbrefId(), null);
        Player player = playerMapper.toEntity(request);
        return enrich(playerRepository.save(player));
    }

    @Override
    @Transactional
    public BulkImportResponse<PlayerResponse> createAll(List<CreatePlayerRequest> requests) {
        return bulkImporter.importAll(requests, new PlayerBulkSpec());
    }

    @Override
    public Page<PlayerResponse> findAll(
            String query,
            String position,
            String league,
            Integer minAge,
            Integer maxAge,
            Pageable pageable
    ) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasPosition = position != null && !position.isBlank();
        boolean hasLeague = league != null && !league.isBlank();
        boolean hasAge = minAge != null || maxAge != null;

        Page<Player> page;
        if (!hasQuery && !hasPosition && !hasLeague && !hasAge) {
            page = playerRepository.findAll(pageable);
        } else {
            Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            String normalized = hasQuery ? SearchQueries.normalize(query) : "";
            Set<String> codes = hasQuery ? FootballCountryNames.codesMatchingQuery(normalized) : Set.of();
            boolean hasCodes = !codes.isEmpty();
            Collection<String> codeParams = hasCodes ? codes : List.of("__none__");
            Collection<String> positions = resolvePositionFilter(position);
            boolean hasPositions = !positions.isEmpty();
            Collection<String> positionParams = hasPositions ? positions : List.of("__none__");

            LocalDate today = LocalDate.now();
            LocalDate bornOnOrBefore = minAge == null ? today : today.minusYears(minAge);
            LocalDate bornOnOrAfter = maxAge == null ? LocalDate.of(1900, 1, 1) : today.minusYears(maxAge + 1L).plusDays(1);
            String leagueParam = hasLeague ? league.trim() : "";

            page = playerRepository.searchFiltered(
                    hasQuery,
                    hasQuery ? SearchQueries.escapeLike(normalized) : "",
                    hasCodes,
                    codeParams,
                    hasPositions,
                    positionParams,
                    minAge != null,
                    bornOnOrBefore,
                    maxAge != null,
                    bornOnOrAfter,
                    hasLeague,
                    leagueParam,
                    unsorted
            );
        }
        Map<UUID, LatestClubView> latestClubs = latestClubsByPlayerId(page.getContent());
        return page.map(player -> playerMapper.toResponse(player, latestClubs.get(player.getId())));
    }

    private static Collection<String> resolvePositionFilter(String position) {
        if (position == null || position.isBlank()) {
            return List.of();
        }
        String key = position.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "DEF", "DEFENDER" -> List.of("RB", "CB", "LB", "RWB", "LWB");
            case "MID", "MIDFIELDER" -> List.of("CDM", "CM", "CAM", "RM", "LM");
            case "FWD", "FORWARD", "ATT", "ATTACKER" -> List.of("RW", "LW", "CF", "ST");
            default -> List.of(key);
        };
    }

    @Override
    public PlayerResponse findById(UUID id) {
        return enrich(findPlayer(id));
    }

    @Override
    @Transactional
    public PlayerResponse update(UUID id, UpdatePlayerRequest request) {
        Player player = findPlayer(id);
        assertUnique(request.fullName(), request.dateOfBirth(), request.nationality(), request.fbrefId(), id);
        playerMapper.updateEntity(player, request);
        return enrich(player);
    }

    @Override
    @Transactional
    public PlayerResponse updateMedia(UUID id, UpdateIdentityMediaRequest request) {
        Player player = findPlayer(id);
        playerMapper.updateMedia(player, request);
        return enrich(player);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findPlayer(id).softDelete();
    }

    private PlayerResponse enrich(Player player) {
        Map<UUID, LatestClubView> latestClubs = latestClubsByPlayerId(List.of(player));
        return playerMapper.toResponse(player, latestClubs.get(player.getId()));
    }

    private Map<UUID, LatestClubView> latestClubsByPlayerId(Collection<Player> players) {
        if (players.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = players.stream().map(Player::getId).toList();
        return playerSeasonRepository.findLatestClubsByPlayerIds(ids).stream()
                .collect(Collectors.toMap(LatestClubView::getPlayerId, Function.identity(), (a, b) -> a));
    }

    private Player findPlayer(UUID id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Player", id));
    }

    private void assertUnique(
            String fullName,
            java.time.LocalDate dateOfBirth,
            String nationality,
            String fbrefId,
            UUID excludingId
    ) {
        String normalizedName = fullName.trim().toLowerCase(Locale.ROOT);
        String normalizedNationality = nationality.trim().toUpperCase(Locale.ROOT);
        boolean nameTaken = excludingId == null
                ? playerRepository.existsByFullNameNormalizedAndDateOfBirthAndNationality(
                        normalizedName, dateOfBirth, normalizedNationality)
                : playerRepository.existsByFullNameNormalizedAndDateOfBirthAndNationalityAndIdNot(
                        normalizedName, dateOfBirth, normalizedNationality, excludingId);
        if (nameTaken) {
            throw new ConflictException(
                    "Player already exists for name/dateOfBirth/nationality: "
                            + fullName + " / " + dateOfBirth + " / " + normalizedNationality
            );
        }

        if (fbrefId == null || fbrefId.isBlank()) {
            return;
        }
        String normalizedFbref = fbrefId.trim();
        boolean fbrefTaken = excludingId == null
                ? playerRepository.existsByFbrefId(normalizedFbref)
                : playerRepository.existsByFbrefIdAndIdNot(normalizedFbref, excludingId);
        if (fbrefTaken) {
            throw new ConflictException("Player already exists for fbrefId: " + normalizedFbref);
        }
    }

    private final class PlayerBulkSpec implements BulkImportSpec<CreatePlayerRequest, PlayerResponse> {

        @Override
        public String naturalKey(CreatePlayerRequest request) {
            if (request.fbrefId() != null && !request.fbrefId().isBlank()) {
                return NaturalKeys.of("fbref", request.fbrefId());
            }
            return NaturalKeys.of(request.fullName(), request.dateOfBirth(), request.nationality());
        }

        @Override
        public String reference(CreatePlayerRequest request) {
            return String.valueOf(request.fullName());
        }

        @Override
        public Set<String> findExistingKeys(List<CreatePlayerRequest> requests) {
            Set<String> existing = new java.util.HashSet<>();

            Set<String> fbrefIds = requests.stream()
                    .map(CreatePlayerRequest::fbrefId)
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toSet());
            if (!fbrefIds.isEmpty()) {
                playerRepository.findAllByFbrefIdIn(fbrefIds).stream()
                        .map(player -> NaturalKeys.of("fbref", player.getFbrefId()))
                        .forEach(existing::add);
            }

            Set<String> names = requests.stream()
                    .map(request -> request.fullName().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            playerRepository.findAllByNormalizedName(names).stream()
                    .map(player -> player.getFbrefId() != null
                            ? NaturalKeys.of("fbref", player.getFbrefId())
                            : NaturalKeys.of(
                                    player.getFullName(),
                                    player.getDateOfBirth(),
                                    player.getNationality()))
                    .forEach(existing::add);

            return existing;
        }

        @Override
        public List<PlayerResponse> persist(List<CreatePlayerRequest> accepted) {
            List<Player> players = accepted.stream().map(playerMapper::toEntity).toList();
            List<Player> saved = playerRepository.saveAll(players);
            Map<UUID, LatestClubView> latestClubs = latestClubsByPlayerId(saved);
            return saved.stream()
                    .map(player -> playerMapper.toResponse(player, latestClubs.get(player.getId())))
                    .toList();
        }
    }
}
