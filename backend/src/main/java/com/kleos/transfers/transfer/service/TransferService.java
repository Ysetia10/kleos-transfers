package com.kleos.transfers.transfer.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.domain.TransferStatus;
import com.kleos.transfers.transfer.dto.CreateTransferRequest;
import com.kleos.transfers.transfer.dto.TransferResponse;
import com.kleos.transfers.transfer.dto.UpdateTransferRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines transfer historical use cases.
 */
public interface TransferService {

    TransferResponse create(CreateTransferRequest request);

    BulkImportResponse<TransferResponse> createAll(List<CreateTransferRequest> requests);

    Page<TransferResponse> findAll(TransferStatus status, UUID seasonId, Pageable pageable);

    TransferResponse findById(UUID id);

    TransferResponse update(UUID id, UpdateTransferRequest request);

    void softDelete(UUID id);
}
