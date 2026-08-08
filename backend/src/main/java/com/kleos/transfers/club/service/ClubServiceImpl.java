package com.kleos.transfers.club.service;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.CurrentManagerView;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.club.entity.Club;
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
import com.kleos.transfers.managerseason.repository.ManagerSeasonRepository;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.playerseason.mapper.PlayerSeasonMapper;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.season.repository.SeasonRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
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
        Map<UUID, CurrentManagerView> managers = currentManagersByClubId(clubs.getContent());
        return clubs.map(club -> clubMapper.toResponse(club, managers.get(club.getId())));
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
        findClub(clubId);
        seasonRepository.findById(seasonId)
                .orElseThrow(() -> ResourceNotFoundException.of("Season", seasonId));
        return playerSeasonRepository.findByClubIdAndSeasonId(clubId, seasonId).stream()
                .sorted(Comparator
                        .comparing((com.kleos.transfers.playerseason.entity.PlayerSeason ps) ->
                                ps.getMinutesPlayed() == null ? 0 : ps.getMinutesPlayed())
                        .reversed()
                        .thenComparing(ps -> ps.getPlayer().getFullName()))
                .map(playerSeasonMapper::toResponse)
                .toList();
    }

    private Club findClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
    }

    private ClubResponse enrich(Club club) {
        Map<UUID, CurrentManagerView> managers = currentManagersByClubId(List.of(club));
        return clubMapper.toResponse(club, managers.get(club.getId()));
    }

    private Map<UUID, CurrentManagerView> currentManagersByClubId(Collection<Club> clubs) {
        if (clubs.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = clubs.stream().map(Club::getId).toList();
        return managerSeasonRepository.findCurrentManagersByClubIds(ids).stream()
                .collect(Collectors.toMap(CurrentManagerView::getClubId, Function.identity(), (a, b) -> a));
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
            Map<UUID, CurrentManagerView> managers = currentManagersByClubId(clubs);
            return clubs.stream()
                    .map(club -> clubMapper.toResponse(club, managers.get(club.getId())))
                    .toList();
        }
    }
}
