package com.kleos.transfers.managerseason.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.managerseason.dto.CreateManagerSeasonRequest;
import com.kleos.transfers.managerseason.dto.ManagerSeasonResponse;
import com.kleos.transfers.managerseason.dto.UpdateManagerSeasonRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines manager-season appointment use cases.
 */
public interface ManagerSeasonService {

    ManagerSeasonResponse create(CreateManagerSeasonRequest request);

    BulkImportResponse<ManagerSeasonResponse> createAll(List<CreateManagerSeasonRequest> requests);

    Page<ManagerSeasonResponse> findAll(Pageable pageable);

    ManagerSeasonResponse findById(UUID id);

    ManagerSeasonResponse update(UUID id, UpdateManagerSeasonRequest request);

    void softDelete(UUID id);
}
