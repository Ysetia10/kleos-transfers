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
import com.kleos.transfers.common.exception.ConflictException;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
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

    @Override
    @Transactional
    public ClubResponse create(CreateClubRequest request) {
        assertUnique(request.name(), request.countryCode(), request.fbrefId(), null);
        Club club = clubMapper.toEntity(request);
        return clubMapper.toResponse(clubRepository.save(club));
    }

    @Override
    @Transactional
    public BulkImportResponse<ClubResponse> createAll(List<CreateClubRequest> requests) {
        return bulkImporter.importAll(requests, new ClubBulkSpec());
    }

    @Override
    public Page<ClubResponse> findAll(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return clubRepository.findAll(pageable).map(clubMapper::toResponse);
        }
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return clubRepository.searchByName(query.trim(), page).map(clubMapper::toResponse);
    }

    @Override
    public ClubResponse findById(UUID id) {
        return clubMapper.toResponse(findClub(id));
    }

    @Override
    @Transactional
    public ClubResponse update(UUID id, UpdateClubRequest request) {
        Club club = findClub(id);
        assertUnique(request.name(), request.countryCode(), request.fbrefId(), id);
        clubMapper.updateEntity(club, request);
        return clubMapper.toResponse(club);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findClub(id).softDelete();
    }

    private Club findClub(UUID id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", id));
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
            List<Club> clubs = accepted.stream().map(clubMapper::toEntity).toList();
            return clubRepository.saveAll(clubs).stream().map(clubMapper::toResponse).toList();
        }
    }
}
