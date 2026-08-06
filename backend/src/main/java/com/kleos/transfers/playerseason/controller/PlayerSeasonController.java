package com.kleos.transfers.playerseason.controller;

import com.kleos.transfers.common.bulk.BulkImportRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.playerseason.dto.CreatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.playerseason.dto.UpdatePlayerSeasonRequest;
import com.kleos.transfers.playerseason.service.PlayerSeasonService;
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
 * HTTP API for player-season performance records.
 */
@RestController
@RequestMapping("/api/v1/player-seasons")
@RequiredArgsConstructor
public class PlayerSeasonController {

    private final PlayerSeasonService playerSeasonService;

    @PostMapping
    public ResponseEntity<PlayerSeasonResponse> create(@Valid @RequestBody CreatePlayerSeasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerSeasonService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse<PlayerSeasonResponse>> createAll(
            @Valid @RequestBody BulkImportRequest<CreatePlayerSeasonRequest> request
    ) {
        return ResponseEntity.ok(playerSeasonService.createAll(request.items()));
    }

    @GetMapping
    public ResponseEntity<Page<PlayerSeasonResponse>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(playerSeasonService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerSeasonResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(playerSeasonService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerSeasonResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlayerSeasonRequest request
    ) {
        return ResponseEntity.ok(playerSeasonService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        playerSeasonService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
