package com.kleos.transfers.season.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.season.dto.CreateSeasonRequest;
import com.kleos.transfers.season.dto.SeasonResponse;
import com.kleos.transfers.season.dto.UpdateSeasonRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines season identity use cases.
 */
public interface SeasonService {

    SeasonResponse create(CreateSeasonRequest request);

    BulkImportResponse<SeasonResponse> createAll(List<CreateSeasonRequest> requests);

    Page<SeasonResponse> findAll(Pageable pageable);

    SeasonResponse findById(UUID id);

    SeasonResponse update(UUID id, UpdateSeasonRequest request);

    void softDelete(UUID id);
}
