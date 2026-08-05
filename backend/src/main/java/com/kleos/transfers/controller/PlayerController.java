package com.kleos.transfers.controller;

import com.kleos.transfers.dto.CreatePlayerRequest;
import com.kleos.transfers.dto.PlayerResponse;
import com.kleos.transfers.dto.UpdatePlayerRequest;
import com.kleos.transfers.service.PlayerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    public ResponseEntity<PlayerResponse> create(@Valid @RequestBody CreatePlayerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PlayerResponse>> findAll() {
        return ResponseEntity.ok(playerService.findAll());
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
}
