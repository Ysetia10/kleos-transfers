package com.kleos.transfers.injury.controller;

import com.kleos.transfers.common.bulk.BulkImportRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.injury.dto.CreateInjuryRequest;
import com.kleos.transfers.injury.dto.InjuryResponse;
import com.kleos.transfers.injury.dto.UpdateInjuryRequest;
import com.kleos.transfers.injury.service.InjuryService;
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
 * HTTP API for injury historical records.
 */
@RestController
@RequestMapping("/api/v1/injuries")
@RequiredArgsConstructor
public class InjuryController {

    private final InjuryService injuryService;

    @PostMapping
    public ResponseEntity<InjuryResponse> create(@Valid @RequestBody CreateInjuryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(injuryService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse<InjuryResponse>> createAll(
            @Valid @RequestBody BulkImportRequest<CreateInjuryRequest> request
    ) {
        return ResponseEntity.ok(injuryService.createAll(request.items()));
    }

    @GetMapping
    public ResponseEntity<Page<InjuryResponse>> findAll(
            @RequestParam(required = false) UUID playerId,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (playerId != null) {
            return ResponseEntity.ok(injuryService.findByPlayerId(playerId, pageable));
        }
        return ResponseEntity.ok(injuryService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InjuryResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(injuryService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InjuryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInjuryRequest request
    ) {
        return ResponseEntity.ok(injuryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        injuryService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
