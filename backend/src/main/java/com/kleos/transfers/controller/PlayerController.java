package com.kleos.transfers.controller;

import com.kleos.transfers.dto.CreatePlayerRequest;
import com.kleos.transfers.dto.PlayerResponse;
import com.kleos.transfers.dto.UpdatePlayerRequest;
import com.kleos.transfers.service.PlayerService;
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
 * HTTP API for player identity records.
 */
@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    public ResponseEntity<PlayerResponse> create(@Valid @RequestBody CreatePlayerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<PlayerResponse>> findAll(
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(playerService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(playerService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlayerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlayerRequest request
    ) {
        return ResponseEntity.ok(playerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        playerService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
