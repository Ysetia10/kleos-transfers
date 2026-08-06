package com.kleos.transfers.manager.controller;

import com.kleos.transfers.manager.dto.CreateManagerRequest;
import com.kleos.transfers.manager.dto.ManagerResponse;
import com.kleos.transfers.manager.dto.UpdateManagerRequest;
import com.kleos.transfers.manager.service.ManagerService;
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
 * HTTP API for manager identity records.
 */
@RestController
@RequestMapping("/api/v1/managers")
@RequiredArgsConstructor
public class ManagerController {

    private final ManagerService managerService;

    @PostMapping
    public ResponseEntity<ManagerResponse> create(@Valid @RequestBody CreateManagerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<ManagerResponse>> findAll(
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(managerService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManagerResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(managerService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ManagerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateManagerRequest request
    ) {
        return ResponseEntity.ok(managerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        managerService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
