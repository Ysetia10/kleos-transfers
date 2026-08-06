package com.kleos.transfers.tournament.controller;

import com.kleos.transfers.common.bulk.BulkImportRequest;
import com.kleos.transfers.common.bulk.BulkImportResponse;
import com.kleos.transfers.tournament.dto.CreateTournamentRequest;
import com.kleos.transfers.tournament.dto.TournamentResponse;
import com.kleos.transfers.tournament.dto.UpdateTournamentRequest;
import com.kleos.transfers.tournament.service.TournamentService;
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
 * HTTP API for tournament identity records.
 */
@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @PostMapping
    public ResponseEntity<TournamentResponse> create(@Valid @RequestBody CreateTournamentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tournamentService.create(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkImportResponse<TournamentResponse>> createAll(
            @Valid @RequestBody BulkImportRequest<CreateTournamentRequest> request
    ) {
        return ResponseEntity.ok(tournamentService.createAll(request.items()));
    }

    @GetMapping
    public ResponseEntity<Page<TournamentResponse>> findAll(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(tournamentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TournamentResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTournamentRequest request
    ) {
        return ResponseEntity.ok(tournamentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        tournamentService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
