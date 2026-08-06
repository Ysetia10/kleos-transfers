package com.kleos.transfers.prediction.engine;

/**
 * Produces explainable prediction metrics for a loaded scenario context.
 */
public interface PredictionEngine {

    String modelVersion();

    EngineResult predict(PredictionContext context);
}
