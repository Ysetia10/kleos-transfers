package com.kleos.transfers.prediction.repository;

import com.kleos.transfers.prediction.entity.PredictionRun;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence access for prediction runs.
 */
public interface PredictionRunRepository extends JpaRepository<PredictionRun, UUID> {
}
