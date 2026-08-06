package com.kleos.transfers.playerseason.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.playerseason.dto.CreatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.playerseason.dto.UpdatePlayerSeasonRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines player-season performance use cases.
 */
public interface PlayerSeasonService {

    PlayerSeasonResponse create(CreatePlayerSeasonRequest request);

    BulkImportResponse<PlayerSeasonResponse> createAll(List<CreatePlayerSeasonRequest> requests);

    Page<PlayerSeasonResponse> findAll(Pageable pageable);

    PlayerSeasonResponse findById(UUID id);

    PlayerSeasonResponse update(UUID id, UpdatePlayerSeasonRequest request);

    void softDelete(UUID id);
}
