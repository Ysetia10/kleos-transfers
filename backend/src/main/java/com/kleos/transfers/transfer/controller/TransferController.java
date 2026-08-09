package com.kleos.transfers.transfer.controller;

import com.kleos.transfers.common.bulk.BulkImportRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.domain.TransferStatus;
import com.kleos.transfers.transfer.dto.CreateTransferRequest;
import com.kleos.transfers.transfer.dto.TransferResponse;
import com.kleos.transfers.transfer.dto.UpdateTransferRequest;
import com.kleos.transfers.transfer.service.TransferService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for transfer historical records.
 */
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse<TransferResponse>> createAll(
            @Valid @RequestBody BulkImportRequest<CreateTransferRequest> request
    ) {
        return ResponseEntity.ok(transferService.createAll(request.items()));
    }

    @GetMapping
    public ResponseEntity<Page<TransferResponse>> findAll(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) UUID seasonId,
            @PageableDefault(size = 20, sort = "transferDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(transferService.findAll(status, seasonId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(transferService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransferResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransferRequest request
    ) {
        return ResponseEntity.ok(transferService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        transferService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
