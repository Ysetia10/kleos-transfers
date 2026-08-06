package com.kleos.transfers.managerseason.controller;

import com.kleos.transfers.common.bulk.BulkImportRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.managerseason.dto.CreateManagerSeasonRequest;
import com.kleos.transfers.managerseason.dto.ManagerSeasonResponse;
import com.kleos.transfers.managerseason.dto.UpdateManagerSeasonRequest;
import com.kleos.transfers.managerseason.service.ManagerSeasonService;
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
 * HTTP API for manager-season appointment records.
 */
@RestController
@RequestMapping("/api/v1/manager-seasons")
@RequiredArgsConstructor
public class ManagerSeasonController {

    private final ManagerSeasonService managerSeasonService;

    @PostMapping
    public ResponseEntity<ManagerSeasonResponse> create(@Valid @RequestBody CreateManagerSeasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerSeasonService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse<ManagerSeasonResponse>> createAll(
            @Valid @RequestBody BulkImportRequest<CreateManagerSeasonRequest> request
    ) {
        return ResponseEntity.ok(managerSeasonService.createAll(request.items()));
    }

    @GetMapping
    public ResponseEntity<Page<ManagerSeasonResponse>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(managerSeasonService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManagerSeasonResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(managerSeasonService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManagerSeasonResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateManagerSeasonRequest request
    ) {
        return ResponseEntity.ok(managerSeasonService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        managerSeasonService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
