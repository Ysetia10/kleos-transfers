package com.kleos.transfers.prediction.controller;

import com.kleos.transfers.prediction.dto.CreatePredictionRequest;
import com.kleos.transfers.prediction.dto.PredictionResponse;
import com.kleos.transfers.prediction.dto.PredictionRunResponse;
import com.kleos.transfers.prediction.service.PredictionService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for transfer what-if predictions.
 *
 * <p>Product surface is scenario-first: create a prediction, read explanations,
 * optionally evaluate against a completed PlayerSeason.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @PostMapping("/predictions")
    public ResponseEntity<PredictionResponse> create(@Valid @RequestBody CreatePredictionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(predictionService.create(request));
    }

    @GetMapping("/predictions")
    public ResponseEntity<Page<PredictionResponse>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(predictionService.findAll(pageable));
    }

    @GetMapping("/predictions/{id}")
    public ResponseEntity<PredictionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(predictionService.findById(id));
    }

    @PostMapping("/predictions/{id}/evaluate")
    public ResponseEntity<PredictionResponse> evaluate(@PathVariable UUID id) {
        return ResponseEntity.ok(predictionService.evaluate(id));
    }

    @DeleteMapping("/predictions/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        predictionService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/prediction-runs/{id}")
    public ResponseEntity<PredictionRunResponse> findRunById(@PathVariable UUID id) {
        return ResponseEntity.ok(predictionService.findRunById(id));
    }
}
