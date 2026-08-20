package com.kleos.transfers.stats.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.stats.dto.ModelAccuracyResponse;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Serves the published league-level backtest summary written by
 * {@code scripts/validate_predictions_season.py}.
 */
@Service
public class ModelAccuracyService {

    private static final String RESOURCE = "model-accuracy/latest.json";

    private final ObjectMapper objectMapper;

    public ModelAccuracyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ModelAccuracyResponse latest() {
        ClassPathResource resource = new ClassPathResource(RESOURCE);
        if (!resource.exists()) {
            throw ResourceNotFoundException.of("ModelAccuracy", "latest");
        }
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, ModelAccuracyResponse.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read published model accuracy artifact", exception);
        }
    }
}
