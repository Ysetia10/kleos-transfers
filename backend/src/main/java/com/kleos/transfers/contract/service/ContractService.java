package com.kleos.transfers.contract.service;

import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.contract.dto.ContractResponse;
import com.kleos.transfers.contract.dto.CreateContractRequest;
import com.kleos.transfers.contract.dto.UpdateContractRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Defines contract historical use cases.
 */
public interface ContractService {

    ContractResponse create(CreateContractRequest request);

    BulkImportResponse<ContractResponse> createAll(List<CreateContractRequest> requests);

    Page<ContractResponse> findAll(Pageable pageable);

    ContractResponse findById(UUID id);

    ContractResponse update(UUID id, UpdateContractRequest request);

    void softDelete(UUID id);
}
