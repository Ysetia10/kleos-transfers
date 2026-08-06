package com.kleos.transfers.manager.service;

import com.kleos.transfers.manager.dto.CreateManagerRequest;
import com.kleos.transfers.manager.dto.ManagerResponse;
import com.kleos.transfers.manager.dto.UpdateManagerRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines manager identity use cases.
 */
public interface ManagerService {

    ManagerResponse create(CreateManagerRequest request);

    Page<ManagerResponse> findAll(Pageable pageable);

    ManagerResponse findById(UUID id);

    ManagerResponse update(UUID id, UpdateManagerRequest request);

    void softDelete(UUID id);
}
