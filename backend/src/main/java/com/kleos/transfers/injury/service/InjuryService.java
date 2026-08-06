package com.kleos.transfers.injury.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.injury.dto.CreateInjuryRequest;
import com.kleos.transfers.injury.dto.InjuryResponse;
import com.kleos.transfers.injury.dto.UpdateInjuryRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines injury historical use cases.
 */
public interface InjuryService {

    InjuryResponse create(CreateInjuryRequest request);

    BulkImportResponse<InjuryResponse> createAll(List<CreateInjuryRequest> requests);

    Page<InjuryResponse> findAll(Pageable pageable);

    InjuryResponse findById(UUID id);

    InjuryResponse update(UUID id, UpdateInjuryRequest request);

    void softDelete(UUID id);
}
