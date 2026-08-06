package com.kleos.transfers.season.controller;

import com.kleos.transfers.common.bulk.BulkImportRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.season.dto.CreateSeasonRequest;
import com.kleos.transfers.season.dto.SeasonResponse;
import com.kleos.transfers.season.dto.UpdateSeasonRequest;
import com.kleos.transfers.season.service.SeasonService;
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
 * HTTP API for season identity records.
 */
@RestController
@RequestMapping("/api/v1/seasons")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;

    @PostMapping
    public ResponseEntity<SeasonResponse> create(@Valid @RequestBody CreateSeasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seasonService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse<SeasonResponse>> createAll(
            @Valid @RequestBody BulkImportRequest<CreateSeasonRequest> request
    ) {
        return ResponseEntity.ok(seasonService.createAll(request.items()));
    }

    @GetMapping
    public ResponseEntity<Page<SeasonResponse>> findAll(
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(seasonService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeasonResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(seasonService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SeasonResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSeasonRequest request
    ) {
        return ResponseEntity.ok(seasonService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        seasonService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
