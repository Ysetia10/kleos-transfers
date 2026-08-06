package com.kleos.transfers.clubseason.service;

import com.kleos.transfers.clubseason.dto.ClubSeasonResponse;
import com.kleos.transfers.clubseason.dto.CreateClubSeasonRequest;
import com.kleos.transfers.clubseason.dto.UpdateClubSeasonRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines club-season historical use cases.
 */
public interface ClubSeasonService {

    ClubSeasonResponse create(CreateClubSeasonRequest request);

    BulkImportResponse<ClubSeasonResponse> createAll(List<CreateClubSeasonRequest> requests);

    Page<ClubSeasonResponse> findAll(Pageable pageable);

    ClubSeasonResponse findById(UUID id);

    ClubSeasonResponse update(UUID id, UpdateClubSeasonRequest request);

    void softDelete(UUID id);
}
