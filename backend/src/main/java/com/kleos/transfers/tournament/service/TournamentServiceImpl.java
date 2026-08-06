package com.kleos.transfers.tournament.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.tournament.dto.CreateTournamentRequest;
import com.kleos.transfers.tournament.dto.TournamentResponse;
import com.kleos.transfers.tournament.dto.UpdateTournamentRequest;
import com.kleos.transfers.tournament.entity.Tournament;
import com.kleos.transfers.tournament.mapper.TournamentMapper;
import com.kleos.transfers.tournament.repository.TournamentRepository;
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
 * Application service for tournament identity use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentMapper tournamentMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public TournamentResponse create(CreateTournamentRequest request) {
        Tournament tournament = tournamentMapper.toEntity(request);
        return tournamentMapper.toResponse(tournamentRepository.save(tournament));
    }

    @Override
    @Transactional
    public BulkImportResponse<TournamentResponse> createAll(List<CreateTournamentRequest> requests) {
        return bulkImporter.importAll(requests, new TournamentBulkSpec());
    }

    @Override
    public Page<TournamentResponse> findAll(Pageable pageable) {
        return tournamentRepository.findAll(pageable).map(tournamentMapper::toResponse);
    }

    @Override
    public TournamentResponse findById(UUID id) {
        return tournamentMapper.toResponse(findTournament(id));
    }

    @Override
    @Transactional
    public TournamentResponse update(UUID id, UpdateTournamentRequest request) {
        Tournament tournament = findTournament(id);
        tournamentMapper.updateEntity(tournament, request);
        return tournamentMapper.toResponse(tournament);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        Tournament tournament = findTournament(id);
        tournament.softDelete();
    }

    private Tournament findTournament(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tournament", id));
    }

    /**
     * Mirrors the database uniqueness rule for active tournaments: normalized name.
     */
    private final class TournamentBulkSpec implements BulkImportSpec<CreateTournamentRequest, TournamentResponse> {

        @Override
        public String naturalKey(CreateTournamentRequest request) {
            return NaturalKeys.of(request.name());
        }

        @Override
        public String reference(CreateTournamentRequest request) {
            return String.valueOf(request.name());
        }

        @Override
        public Set<String> findExistingKeys(List<CreateTournamentRequest> requests) {
            Set<String> names = requests.stream()
                    .map(request -> request.name().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            return tournamentRepository.findAllByNormalizedName(names).stream()
                    .map(tournament -> NaturalKeys.of(tournament.getName()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<TournamentResponse> persist(List<CreateTournamentRequest> accepted) {
            List<Tournament> tournaments = accepted.stream().map(tournamentMapper::toEntity).toList();
            return tournamentRepository.saveAll(tournaments).stream()
                    .map(tournamentMapper::toResponse)
                    .toList();
        }
    }
}
