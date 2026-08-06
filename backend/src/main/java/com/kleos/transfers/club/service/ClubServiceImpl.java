package com.kleos.transfers.club.service;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.mapper.ClubMapper;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.common.bulk.BulkImportSpec;
import com.kleos.transfers.common.bulk.BulkImporter;
import com.kleos.transfers.common.bulk.NaturalKeys;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
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
 * Application service for club identity use cases.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepository;
    private final ClubMapper clubMapper;
    private final BulkImporter bulkImporter;

    @Override
    @Transactional
    public ClubResponse create(CreateClubRequest request) {
        Club club = clubMapper.toEntity(request);
        return clubMapper.toResponse(clubRepository.save(club));
    }

    @Override
    @Transactional
    public BulkImportResponse<ClubResponse> createAll(List<CreateClubRequest> requests) {
        return bulkImporter.importAll(requests, new ClubBulkSpec());
    }

    @Override
    public Page<ClubResponse> findAll(Pageable pageable) {
        return clubRepository.findAll(pageable).map(clubMapper::toResponse);
    }

    @Override
    public ClubResponse findById(UUID id) {
        return clubMapper.toResponse(findClub(id));
    }

    @Override
    @Transactional
    public ClubResponse update(UUID id, UpdateClubRequest request) {
        Club club = findClub(id);
        clubMapper.updateEntity(club, request);
        return clubMapper.toResponse(club);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        Club club = findClub(id);
        club.softDelete();
    }

    private Club findClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
    }

    /**
     * Mirrors the database uniqueness rule for active clubs: name plus country.
     */
    private final class ClubBulkSpec implements BulkImportSpec<CreateClubRequest, ClubResponse> {

        @Override
        public String naturalKey(CreateClubRequest request) {
            return NaturalKeys.of(request.name(), request.countryCode());
        }

        @Override
        public String reference(CreateClubRequest request) {
            return String.valueOf(request.name());
        }

        @Override
        public Set<String> findExistingKeys(List<CreateClubRequest> requests) {
            Set<String> names = requests.stream()
                    .map(request -> request.name().trim().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            return clubRepository.findAllByNormalizedName(names).stream()
                    .map(club -> NaturalKeys.of(club.getName(), club.getCountryCode()))
                    .collect(Collectors.toSet());
        }

        @Override
        public List<ClubResponse> persist(List<CreateClubRequest> accepted) {
            List<Club> clubs = accepted.stream().map(clubMapper::toEntity).toList();
            return clubRepository.saveAll(clubs).stream().map(clubMapper::toResponse).toList();
        }
    }
}
