package com.kleos.transfers.clubseason.controller;

import com.kleos.transfers.clubseason.dto.ClubSeasonResponse;
import com.kleos.transfers.clubseason.dto.CreateClubSeasonRequest;
import com.kleos.transfers.clubseason.dto.UpdateClubSeasonRequest;
import com.kleos.transfers.clubseason.service.ClubSeasonService;
import com.kleos.transfers.common.bulk.BulkImportRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
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
 * HTTP API for club-season historical records.
 */
@RestController
@RequestMapping("/api/v1/club-seasons")
@RequiredArgsConstructor
public class ClubSeasonController {

    private final ClubSeasonService clubSeasonService;

    @PostMapping
    public ResponseEntity<ClubSeasonResponse> create(@Valid @RequestBody CreateClubSeasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clubSeasonService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse<ClubSeasonResponse>> createAll(
            @Valid @RequestBody BulkImportRequest<CreateClubSeasonRequest> request
    ) {
        return ResponseEntity.ok(clubSeasonService.createAll(request.items()));
    }

    @GetMapping
    public ResponseEntity<Page<ClubSeasonResponse>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(clubSeasonService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClubSeasonResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(clubSeasonService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClubSeasonResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClubSeasonRequest request
    ) {
        return ResponseEntity.ok(clubSeasonService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        clubSeasonService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
