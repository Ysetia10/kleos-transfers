package com.kleos.transfers.club.service;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.ClubSquadSeasonStatsView;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.CurrentManagerView;
import com.kleos.transfers.club.dto.LikelyLineupResponse;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.lineup.LikelyLineupAnalyzer;
import com.kleos.transfers.club.mapper.ClubMapper;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ConflictException;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.common.dto.UpdateIdentityMediaRequest;
import com.kleos.transfers.common.search.SearchQueries;
import com.kleos.transfers.domain.FootballCountryNames;
import com.kleos.transfers.domain.TacticalSystem;
import com.kleos.transfers.domain.TempoProfile;
import com.kleos.transfers.domain.TransferStatus;
import com.kleos.transfers.managerseason.repository.ManagerSeasonRepository;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.playerseason.mapper.PlayerSeasonMapper;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.transfer.dto.TransferMoveSummary;
import com.kleos.transfers.transfer.entity.Transfer;
import com.kleos.transfers.transfer.mapper.TransferMapper;
import com.kleos.transfers.transfer.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
 * Application service for club identity use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepository;
    private final ClubMapper clubMapper;
    private final BulkImporter bulkImporter;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final PlayerSeasonMapper playerSeasonMapper;
    private final SeasonRepository seasonRepository;
    private final ManagerSeasonRepository managerSeasonRepository;
    private final TransferRepository transferRepository;
    private final TransferMapper transferMapper;
    private final LikelyLineupAnalyzer likelyLineupAnalyzer;

    private static final List<TransferStatus> SQUAD_PROJECTION_STATUSES = List.of(
            TransferStatus.COMPLETED,
            TransferStatus.ANNOUNCED
    );

    @Override
    @Transactional
    public ClubResponse create(CreateClubRequest request) {
        assertUnique(request.name(), request.countryCode(), request.fbrefId(), null);
        Club club = clubMapper.toEntity(request);
        return enrich(clubRepository.save(club));
    }

    @Override
    @Transactional
    public BulkImportResponse<ClubResponse> createAll(List<CreateClubRequest> requests) {
        return bulkImporter.importAll(requests, new ClubBulkSpec());
    }

    @Override
    public Page<ClubResponse> findAll(String query, Pageable pageable) {
        Page<Club> clubs;
        if (query == null || query.isBlank()) {
            clubs = clubRepository.findAll(pageable);
        } else {
            Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            String normalized = SearchQueries.normalize(query);
            Set<String> codes = FootballCountryNames.codesMatchingQuery(normalized);
            boolean hasCodes = !codes.isEmpty();
            Collection<String> codeParams = hasCodes ? codes : List.of("__none__");
            clubs = clubRepository.search(
                    SearchQueries.escapeLike(normalized),
                    hasCodes,
                    codeParams,
                    page
            );
        }
        return mapClubs(clubs.getContent(), clubs);
    }

    @Override
    public ClubResponse findById(UUID id) {
        return enrich(findClub(id));
    }

    @Override
    @Transactional
    public ClubResponse update(UUID id, UpdateClubRequest request) {
        Club club = findClub(id);
        assertUnique(request.name(), request.countryCode(), request.fbrefId(), id);
        clubMapper.updateEntity(club, request);
        return enrich(club);
    }

    @Override
    @Transactional
    public ClubResponse updateMedia(UUID id, UpdateIdentityMediaRequest request) {
        Club club = findClub(id);
        clubMapper.updateMedia(club, request);
        return enrich(club);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findClub(id).softDelete();
    }

    @Override
    public List<PlayerSeasonResponse> findSquad(UUID clubId, UUID seasonId) {
        Club club = findClub(clubId);
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> ResourceNotFoundException.of("Season", seasonId));

        List<PlayerSeason> direct = playerSeasonRepository.findByClubIdAndSeasonId(clubId, seasonId);
        if (!direct.isEmpty()) {
            Map<UUID, TransferMoveSummary> inbound = inboundTransfersByPlayer(club.getId(), season.getId());
            return sortMappedSquad(direct.stream()
                    .map(row -> playerSeasonMapper.toResponse(row, inbound.get(row.getPlayer().getId())))
                    .toList());
        }

        return projectSquadFromPriorSeason(club, season);
    }

    @Override
    public LikelyLineupResponse findLikelyLineup(UUID clubId, UUID seasonId) {
        Club club = findClub(clubId);
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> ResourceNotFoundException.of("Season", seasonId));
        List<PlayerSeasonResponse> projected = findSquad(clubId, seasonId);
        List<PlayerSeasonResponse> prior = loadPriorSeasonSquad(club, season);
        if (prior.isEmpty()) {
            prior = projected;
        }
        return likelyLineupAnalyzer.analyze(prior, projected);
    }

    private List<PlayerSeasonResponse> loadPriorSeasonSquad(Club club, Season season) {
        return seasonRepository.findFirstByStartDateLessThanOrderByStartDateDesc(season.getStartDate())
                .map(prior -> sortMappedSquad(playerSeasonRepository.findByClubIdAndSeasonId(club.getId(), prior.getId())
                        .stream()
                        .map(playerSeasonMapper::toResponse)
                        .toList()))
                .orElse(List.of());
    }

    /**
     * When the requested season has no PlayerSeason rows (typical for an upcoming campaign),
     * build a working squad from the previous season's roster minus outs plus ins for that season.
     */
    private List<PlayerSeasonResponse> projectSquadFromPriorSeason(Club club, Season season) {
        Optional<Season> priorOpt = seasonRepository
                .findFirstByStartDateLessThanOrderByStartDateDesc(season.getStartDate());
        if (priorOpt.isEmpty()) {
            return List.of();
        }

        Season prior = priorOpt.get();
        List<PlayerSeason> base = playerSeasonRepository.findByClubIdAndSeasonId(club.getId(), prior.getId());
        if (base.isEmpty()) {
            return List.of();
        }

        List<Transfer> transfers = transferRepository.findBySeasonIdAndClubIdAndStatusIn(
                season.getId(),
                club.getId(),
                SQUAD_PROJECTION_STATUSES
        );

        Set<UUID> departedPlayerIds = new HashSet<>();
        Map<UUID, Transfer> arrivalsByPlayerId = new LinkedHashMap<>();
        for (Transfer transfer : transfers) {
            UUID playerId = transfer.getPlayer().getId();
            if (transfer.getFromClub() != null && club.getId().equals(transfer.getFromClub().getId())) {
                departedPlayerIds.add(playerId);
            }
            if (transfer.getToClub() != null && club.getId().equals(transfer.getToClub().getId())) {
                arrivalsByPlayerId.put(playerId, transfer);
            }
        }

        List<PlayerSeasonResponse> projected = new ArrayList<>();
        for (PlayerSeason row : base) {
            UUID playerId = row.getPlayer().getId();
            if (departedPlayerIds.contains(playerId) && !arrivalsByPlayerId.containsKey(playerId)) {
                continue;
            }
            Transfer arrival = arrivalsByPlayerId.remove(playerId);
            TransferMoveSummary inbound = arrival == null ? null : transferMapper.toMoveSummary(arrival);
            projected.add(playerSeasonMapper.toProjectedResponse(row, club, season, inbound));
        }

        for (Transfer arrival : arrivalsByPlayerId.values()) {
            Player player = arrival.getPlayer();
            List<PlayerSeason> history = playerSeasonRepository.findHistoryByPlayerIdBefore(
                    player.getId(),
                    season.getStartDate()
            );
            PlayerSeason priorStats = history.isEmpty() ? null : history.getFirst();
            projected.add(playerSeasonMapper.toProjectedArrival(
                    player,
                    club,
                    season,
                    priorStats,
                    transferMapper.toMoveSummary(arrival)
            ));
        }

        return sortMappedSquad(projected);
    }

    private Map<UUID, TransferMoveSummary> inboundTransfersByPlayer(UUID clubId, UUID seasonId) {
        List<Transfer> transfers = transferRepository.findBySeasonIdAndClubIdAndStatusIn(
                seasonId,
                clubId,
                SQUAD_PROJECTION_STATUSES
        );
        Map<UUID, TransferMoveSummary> inbound = new HashMap<>();
        for (Transfer transfer : transfers) {
            if (transfer.getToClub() != null && clubId.equals(transfer.getToClub().getId())) {
                inbound.put(transfer.getPlayer().getId(), transferMapper.toMoveSummary(transfer));
            }
        }
        return inbound;
    }

    private static List<PlayerSeasonResponse> sortMappedSquad(List<PlayerSeasonResponse> squad) {
        return squad.stream()
                .sorted(Comparator
                        .comparing((PlayerSeasonResponse row) ->
                                row.minutesPlayed() == null ? 0 : row.minutesPlayed())
                        .reversed()
                        .thenComparing(PlayerSeasonResponse::playerName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private Club findClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
    }

    private ClubResponse enrich(Club club) {
        return mapClubs(List.of(club), null).getContent().getFirst();
    }

    private Page<ClubResponse> mapClubs(List<Club> clubs, Page<Club> page) {
        Map<UUID, CurrentManagerView> managers = currentManagersByClubId(clubs);
        Map<UUID, ClubSquadSeasonStatsView> squadStats = squadStatsForManagers(managers.values());

        if (page == null) {
            return new org.springframework.data.domain.PageImpl<>(
                    clubs.stream()
                            .map(club -> toEnrichedResponse(club, managers.get(club.getId()), squadStats))
                            .toList()
            );
        }
        return page.map(club -> toEnrichedResponse(club, managers.get(club.getId()), squadStats));
    }

    private ClubResponse toEnrichedResponse(
            Club club,
            CurrentManagerView manager,
            Map<UUID, ClubSquadSeasonStatsView> squadStats
    ) {
        ClubSquadSeasonStatsView stats = null;
        if (manager != null && manager.getSeasonId() != null) {
            ClubSquadSeasonStatsView candidate = squadStats.get(club.getId());
            if (candidate != null && manager.getSeasonId().equals(candidate.getSeasonId())) {
                stats = candidate;
            }
        }

        TacticalSystem system = parseSystem(manager == null ? null : manager.getTacticalSystem());
        TempoProfile tempo = parseTempo(manager == null ? null : manager.getTempo());
        BigDecimal youth = manager == null ? null : manager.getYouthMinutesPct();
        if (youth == null && stats != null) {
            youth = stats.getYouthMinutesPct();
        }
        // Derive coarse defaults when appointment exists but tactics are not curated yet.
        if (manager != null && system == null) {
            system = TacticalSystem.BALANCED;
        }
        if (manager != null && tempo == null) {
            tempo = defaultTempo(club.getCountryCode());
        }

        boolean hasSquad = stats != null && stats.getPlayerCount() > 0;
        ClubFitIndexCalculator.Result fit = ClubFitIndexCalculator.compute(
                new ClubFitIndexCalculator.Input(
                        manager != null,
                        system,
                        tempo,
                        youth,
                        club.getCountryCode(),
                        hasSquad
                )
        );
        return clubMapper.toResponse(club, manager, system, tempo, youth, hasSquad, fit);
    }

    private Map<UUID, ClubSquadSeasonStatsView> squadStatsForManagers(
            Collection<CurrentManagerView> managers
    ) {
        List<UUID> clubIds = managers.stream()
                .map(CurrentManagerView::getClubId)
                .distinct()
                .toList();
        List<UUID> seasonIds = managers.stream()
                .map(CurrentManagerView::getSeasonId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (clubIds.isEmpty() || seasonIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> expectedSeasonByClub = managers.stream()
                .filter(m -> m.getSeasonId() != null)
                .collect(Collectors.toMap(
                        CurrentManagerView::getClubId,
                        CurrentManagerView::getSeasonId,
                        (a, b) -> a
                ));
        Map<UUID, ClubSquadSeasonStatsView> byClub = new HashMap<>();
        for (ClubSquadSeasonStatsView row : playerSeasonRepository.findSquadSeasonStats(clubIds, seasonIds)) {
            UUID expected = expectedSeasonByClub.get(row.getClubId());
            if (expected != null && expected.equals(row.getSeasonId())) {
                byClub.put(row.getClubId(), row);
            }
        }
        return byClub;
    }

    private Map<UUID, CurrentManagerView> currentManagersByClubId(Collection<Club> clubs) {
        if (clubs.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = clubs.stream().map(Club::getId).toList();
        return managerSeasonRepository.findCurrentManagersByClubIds(ids).stream()
                .collect(Collectors.toMap(CurrentManagerView::getClubId, Function.identity(), (a, b) -> a));
    }

    private static TacticalSystem parseSystem(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return TacticalSystem.valueOf(value);
    }

    private static TempoProfile parseTempo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return TempoProfile.valueOf(value);
    }

    private static TempoProfile defaultTempo(String countryCode) {
        if (countryCode == null) {
            return TempoProfile.MEDIUM;
        }
        return switch (countryCode.toUpperCase(Locale.ROOT)) {
            case "ENG", "GER" -> TempoProfile.HIGH;
            case "ITA" -> TempoProfile.LOW;
            default -> TempoProfile.MEDIUM;
        };
    }

    private void assertUnique(String name, String countryCode, String fbrefId, UUID excludingId) {
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        String normalizedCountry = countryCode.trim().toUpperCase(Locale.ROOT);
        boolean nameTaken = excludingId == null
                ? clubRepository.existsByNameNormalizedAndCountryCode(normalizedName, normalizedCountry)
                : clubRepository.existsByNameNormalizedAndCountryCodeAndIdNot(
                        normalizedName, normalizedCountry, excludingId);
        if (nameTaken) {
            throw new ConflictException(
                    "Club already exists for name/countryCode: " + name + " / " + normalizedCountry
            );
        }

        if (fbrefId == null || fbrefId.isBlank()) {
            return;
        }
        String normalizedFbref = fbrefId.trim();
        boolean fbrefTaken = excludingId == null
                ? clubRepository.existsByFbrefId(normalizedFbref)
                : clubRepository.existsByFbrefIdAndIdNot(normalizedFbref, excludingId);
        if (fbrefTaken) {
            throw new ConflictException("Club already exists for fbrefId: " + normalizedFbref);
        }
    }

    private final class ClubBulkSpec implements BulkImportSpec<CreateClubRequest, ClubResponse> {

        @Override
        public String naturalKey(CreateClubRequest request) {
            if (request.fbrefId() != null && !request.fbrefId().isBlank()) {
                return NaturalKeys.of("fbref", request.fbrefId());
            }
            return NaturalKeys.of(request.name(), request.countryCode());
        }

        @Override
        public String reference(CreateClubRequest request) {
            return String.valueOf(request.name());
        }

        @Override
        public Set<String> findExistingKeys(List<CreateClubRequest> requests) {
            Set<String> existing = new HashSet<>();

            Set<String> fbrefIds = requests.stream()
                    .map(CreateClubRequest::fbrefId)
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toSet());
            if (!fbrefIds.isEmpty()) {
                clubRepository.findAllByFbrefIdIn(fbrefIds).stream()
                        .map(club -> NaturalKeys.of("fbref", club.getFbrefId()))
                        .forEach(existing::add);
            }

            Set<String> names = requests.stream()
                    .map(request -> request.name().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            clubRepository.findAllByNormalizedName(names).stream()
                    .map(club -> club.getFbrefId() != null
                            ? NaturalKeys.of("fbref", club.getFbrefId())
                            : NaturalKeys.of(club.getName(), club.getCountryCode()))
                    .forEach(existing::add);

            return existing;
        }

        @Override
        public List<ClubResponse> persist(List<CreateClubRequest> accepted) {
            List<Club> clubs = clubRepository.saveAll(accepted.stream().map(clubMapper::toEntity).toList());
            return mapClubs(clubs, null).getContent();
        }
    }
}
