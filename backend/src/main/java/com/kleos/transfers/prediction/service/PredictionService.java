package com.kleos.transfers.prediction.service;

import com.kleos.transfers.prediction.dto.CreatePredictionRequest;
import com.kleos.transfers.prediction.dto.PredictionResponse;
import com.kleos.transfers.prediction.dto.PredictionRunResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Prediction use cases: create scenario runs, read results, evaluate against outcomes.
 */
public interface PredictionService {

    PredictionResponse create(CreatePredictionRequest request);

    PredictionResponse findById(UUID id);

    Page<PredictionResponse> findAll(Pageable pageable);

    Page<PredictionResponse> findByPlayerId(UUID playerId, Pageable pageable);

    PredictionRunResponse findRunById(UUID runId);

    PredictionResponse evaluate(UUID predictionId);

    void softDelete(UUID id);
}
