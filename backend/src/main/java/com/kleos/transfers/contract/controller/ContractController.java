package com.kleos.transfers.contract.controller;

import com.kleos.transfers.common.bulk.BulkImportRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.contract.dto.ContractResponse;
import com.kleos.transfers.contract.dto.CreateContractRequest;
import com.kleos.transfers.contract.dto.UpdateContractRequest;
import com.kleos.transfers.contract.service.ContractService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for contract historical records.
 */
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PostMapping
    public ResponseEntity<ContractResponse> create(@Valid @RequestBody CreateContractRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contractService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse<ContractResponse>> createAll(
            @Valid @RequestBody BulkImportRequest<CreateContractRequest> request
    ) {
        return ResponseEntity.ok(contractService.createAll(request.items()));
    }

    @GetMapping
    public ResponseEntity<Page<ContractResponse>> findAll(
            @PageableDefault(size = 20, sort = "endDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(contractService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContractResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContractRequest request
    ) {
        return ResponseEntity.ok(contractService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        contractService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
