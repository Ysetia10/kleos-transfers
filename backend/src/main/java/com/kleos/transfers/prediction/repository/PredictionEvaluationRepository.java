package com.kleos.transfers.prediction.repository;

import com.kleos.transfers.prediction.entity.PredictionEvaluation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for prediction evaluations.
 */
public interface PredictionEvaluationRepository extends JpaRepository<PredictionEvaluation, UUID> {

    boolean existsByPredictionId(UUID predictionId);
}
