package com.kleos.transfers.season.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.season.dto.CreateSeasonRequest;
import com.kleos.transfers.season.dto.SeasonResponse;
import com.kleos.transfers.season.dto.UpdateSeasonRequest;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.mapper.SeasonMapper;
import com.kleos.transfers.season.repository.SeasonRepository;
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
 * Application service for season identity use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeasonServiceImpl implements SeasonService {

    private final SeasonRepository seasonRepository;
    private final SeasonMapper seasonMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public SeasonResponse create(CreateSeasonRequest request) {
        Season season = seasonMapper.toEntity(request);
        return seasonMapper.toResponse(seasonRepository.save(season));
    }

    @Override
    @Transactional
    public BulkImportResponse<SeasonResponse> createAll(List<CreateSeasonRequest> requests) {
        return bulkImporter.importAll(requests, new SeasonBulkSpec());
    }

    @Override
    public Page<SeasonResponse> findAll(Pageable pageable) {
        return seasonRepository.findAll(pageable).map(seasonMapper::toResponse);
    }

    @Override
    public SeasonResponse findById(UUID id) {
        return seasonMapper.toResponse(findSeason(id));
    }

    @Override
    @Transactional
    public SeasonResponse update(UUID id, UpdateSeasonRequest request) {
        Season season = findSeason(id);
        seasonMapper.updateEntity(season, request);
        return seasonMapper.toResponse(season);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        Season season = findSeason(id);
        season.softDelete();
    }

    private Season findSeason(UUID id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Season", id));
    }

    /**
     * Mirrors the database uniqueness rule for active seasons: normalized label.
     */
    private final class SeasonBulkSpec implements BulkImportSpec<CreateSeasonRequest, SeasonResponse> {

        @Override
        public String naturalKey(CreateSeasonRequest request) {
            return NaturalKeys.of(request.label());
        }

        @Override
        public String reference(CreateSeasonRequest request) {
            return String.valueOf(request.label());
        }

        @Override
        public Set<String> findExistingKeys(List<CreateSeasonRequest> requests) {
            Set<String> labels = requests.stream()
                    .map(request -> request.label().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            return seasonRepository.findAllByNormalizedLabel(labels).stream()
                    .map(season -> NaturalKeys.of(season.getLabel()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<SeasonResponse> persist(List<CreateSeasonRequest> accepted) {
            List<Season> seasons = accepted.stream().map(seasonMapper::toEntity).toList();
            return seasonRepository.saveAll(seasons).stream().map(seasonMapper::toResponse).toList();
        }
    }
}
