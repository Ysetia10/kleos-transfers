package com.kleos.transfers.clubseason.service;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.clubseason.dto.ClubSeasonResponse;
import com.kleos.transfers.clubseason.dto.CreateClubSeasonRequest;
import com.kleos.transfers.clubseason.dto.UpdateClubSeasonRequest;
import com.kleos.transfers.clubseason.entity.ClubSeason;
import com.kleos.transfers.clubseason.mapper.ClubSeasonMapper;
import com.kleos.transfers.clubseason.repository.ClubSeasonRepository;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.tournament.entity.Tournament;
import com.kleos.transfers.tournament.repository.TournamentRepository;
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
 * Application service for club-season historical use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubSeasonServiceImpl implements ClubSeasonService {

    private final ClubSeasonRepository clubSeasonRepository;
    private final ClubRepository clubRepository;
    private final SeasonRepository seasonRepository;
    private final TournamentRepository tournamentRepository;
    private final ClubSeasonMapper clubSeasonMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public ClubSeasonResponse create(CreateClubSeasonRequest request) {
        Club club = requireClub(request.clubId());
        Season season = requireSeason(request.seasonId());
        Tournament tournament = requireTournament(request.tournamentId());
        ClubSeason clubSeason = clubSeasonMapper.toEntity(club, season, tournament);
        return clubSeasonMapper.toResponse(clubSeasonRepository.save(clubSeason));
    }

    @Override
    @Transactional
    public BulkImportResponse<ClubSeasonResponse> createAll(List<CreateClubSeasonRequest> requests) {
        return bulkImporter.importAll(requests, new ClubSeasonBulkSpec());
    }

    @Override
    public Page<ClubSeasonResponse> findAll(Pageable pageable) {
        return clubSeasonRepository.findAll(pageable).map(clubSeasonMapper::toResponse);
    }

    @Override
    public ClubSeasonResponse findById(UUID id) {
        return clubSeasonMapper.toResponse(findClubSeason(id));
    }

    @Override
    @Transactional
    public ClubSeasonResponse update(UUID id, UpdateClubSeasonRequest request) {
        ClubSeason clubSeason = findClubSeason(id);
        Club club = requireClub(request.clubId());
        Season season = requireSeason(request.seasonId());
        Tournament tournament = requireTournament(request.tournamentId());
        clubSeason.reassign(club, season, tournament);
        return clubSeasonMapper.toResponse(clubSeason);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findClubSeason(id).softDelete();
    }

    private ClubSeason findClubSeason(UUID id) {
        return clubSeasonRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ClubSeason", id));
    }

    private Club requireClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
    }

    private Season requireSeason(UUID id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Season", id));
    }

    private Tournament requireTournament(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tournament", id));
    }

    private final class ClubSeasonBulkSpec implements BulkImportSpec<CreateClubSeasonRequest, ClubSeasonResponse> {

        @Override
        public String naturalKey(CreateClubSeasonRequest request) {
            return NaturalKeys.of(request.clubId(), request.seasonId());
        }

        @Override
        public String reference(CreateClubSeasonRequest request) {
            return request.clubId() + " / " + request.seasonId();
        }

        @Override
        public Set<String> findExistingKeys(List<CreateClubSeasonRequest> requests) {
            Set<String> keys = requests.stream()
                    .map(request -> request.clubId() + ":" + request.seasonId())
                    .collect(Collectors.toSet());
            return clubSeasonRepository.findAllByUniquenessKeyIn(keys).stream()
                    .map(clubSeason -> NaturalKeys.of(
                            clubSeason.getClub().getId(), clubSeason.getSeason().getId()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<ClubSeasonResponse> persist(List<CreateClubSeasonRequest> accepted) {
            Set<UUID> clubIds = new HashSet<>();
            Set<UUID> seasonIds = new HashSet<>();
            Set<UUID> tournamentIds = new HashSet<>();
            for (CreateClubSeasonRequest request : accepted) {
                clubIds.add(request.clubId());
                seasonIds.add(request.seasonId());
                tournamentIds.add(request.tournamentId());
            }

            Map<UUID, Club> clubs = clubRepository.findAllById(clubIds).stream()
                    .collect(Collectors.toMap(Club::getId, club -> club));
            Map<UUID, Season> seasons = seasonRepository.findAllById(seasonIds).stream()
                    .collect(Collectors.toMap(Season::getId, season -> season));
            Map<UUID, Tournament> tournaments = tournamentRepository.findAllById(tournamentIds).stream()
                    .collect(Collectors.toMap(Tournament::getId, tournament -> tournament));

            List<ClubSeason> entities = accepted.stream()
                    .map(request -> clubSeasonMapper.toEntity(
                            requirePresent(clubs, request.clubId(), "Club"),
                            requirePresent(seasons, request.seasonId(), "Season"),
                            requirePresent(tournaments, request.tournamentId(), "Tournament")))
                    .toList();

            return clubSeasonRepository.saveAll(entities).stream()
                    .map(clubSeasonMapper::toResponse)
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
