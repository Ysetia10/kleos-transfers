package com.kleos.transfers.prediction.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Prediction run with nested prediction summaries.
 */
public record PredictionRunResponse(
        UUID id,
        String modelVersion,
        String note,
        Instant createdAt,
        List<PredictionResponse> predictions
) {
}
