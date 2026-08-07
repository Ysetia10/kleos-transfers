package com.kleos.transfers.club.controller;

import com.kleos.transfers.club.dto.ClubResponse;
import com.kleos.transfers.club.dto.CreateClubRequest;
import com.kleos.transfers.club.dto.UpdateClubRequest;
import com.kleos.transfers.club.service.ClubService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for club identity records.
 */
@RestController
@RequestMapping("/api/v1/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @PostMapping
    public ResponseEntity<ClubResponse> create(@Valid @RequestBody CreateClubRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clubService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse<ClubResponse>> createAll(
            @Valid @RequestBody BulkImportRequest<CreateClubRequest> request
    ) {
        return ResponseEntity.ok(clubService.createAll(request.items()));
    }

    @GetMapping
    public ResponseEntity<Page<ClubResponse>> findAll(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(clubService.findAll(q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClubResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(clubService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClubResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClubRequest request
    ) {
        return ResponseEntity.ok(clubService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        clubService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
