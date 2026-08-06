package com.kleos.transfers.prediction.service;

import com.kleos.transfers.common.exception.ConflictException;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.prediction.dto.CreatePredictionRequest;
import com.kleos.transfers.prediction.dto.PredictionResponse;
import com.kleos.transfers.prediction.dto.PredictionRunResponse;
import com.kleos.transfers.prediction.engine.EngineResult;
import com.kleos.transfers.prediction.engine.PredictionContext;
import com.kleos.transfers.prediction.engine.PredictionContextLoader;
import com.kleos.transfers.prediction.engine.PredictionEngine;
import com.kleos.transfers.prediction.entity.Prediction;
import com.kleos.transfers.prediction.entity.PredictionEvaluation;
import com.kleos.transfers.prediction.entity.PredictionRun;
import com.kleos.transfers.prediction.mapper.PredictionMapper;
import com.kleos.transfers.prediction.repository.PredictionEvaluationRepository;
import com.kleos.transfers.prediction.repository.PredictionRepository;
import com.kleos.transfers.prediction.repository.PredictionRunRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates context loading, engine execution, persistence, and evaluation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PredictionServiceImpl implements PredictionService {

    private final PredictionContextLoader contextLoader;
    private final PredictionEngine predictionEngine;
    private final PredictionRunRepository predictionRunRepository;
    private final PredictionRepository predictionRepository;
    private final PredictionEvaluationRepository predictionEvaluationRepository;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final PredictionMapper predictionMapper;

    @Override
    @Transactional
    public PredictionResponse create(CreatePredictionRequest request) {
        PredictionContext context = contextLoader.load(
                request.playerId(),
                request.targetClubId(),
                request.seasonId()
        );
        EngineResult result = predictionEngine.predict(context);

        PredictionRun run = predictionRunRepository.save(
                new PredictionRun(predictionEngine.modelVersion(), request.note())
        );
        Prediction prediction = predictionMapper.toEntity(
                run,
                result,
                new PredictionMapper.PredictionContextRefs(
                        context.player(),
                        context.targetClub(),
                        context.season()
                )
        );
        predictionMapper.attachExplanations(prediction, result.factors());
        Prediction saved = predictionRepository.save(prediction);
        return predictionMapper.toResponse(saved);
    }

    @Override
    public PredictionResponse findById(UUID id) {
        return predictionMapper.toResponse(findDetailed(id));
    }

    @Override
    public Page<PredictionResponse> findAll(Pageable pageable) {
        return predictionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(predictionMapper::toResponse);
    }

    @Override
    public PredictionRunResponse findRunById(UUID runId) {
        PredictionRun run = predictionRunRepository.findById(runId)
                .orElseThrow(() -> ResourceNotFoundException.of("PredictionRun", runId));
        List<Prediction> predictions = predictionRepository.findDetailedByRunId(runId);
        return predictionMapper.toRunResponse(run, predictions);
    }

    @Override
    @Transactional
    public PredictionResponse evaluate(UUID predictionId) {
        Prediction prediction = findDetailed(predictionId);
        if (prediction.getEvaluation() != null
                || predictionEvaluationRepository.existsByPredictionId(predictionId)) {
            throw new ConflictException("Prediction already evaluated: " + predictionId);
        }

        PlayerSeason outcome = playerSeasonRepository
                .findByPlayerIdAndClubIdAndSeasonId(
                        prediction.getPlayer().getId(),
                        prediction.getTargetClub().getId(),
                        prediction.getSeason().getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No PlayerSeason outcome for player/club/season of prediction " + predictionId
                ));

        PredictionEvaluation evaluation = new PredictionEvaluation(
                prediction,
                outcome.getMinutesPlayed(),
                outcome.getGoals(),
                outcome.getAssists(),
                outcome.getXg(),
                outcome.getXa(),
                outcome.getMinutesPlayed() - prediction.getPredictedMinutes(),
                diff(outcome.getGoals(), prediction.getPredictedGoals()),
                diff(outcome.getAssists(), prediction.getPredictedAssists()),
                outcome.getXg().subtract(prediction.getPredictedXg()).setScale(2, RoundingMode.HALF_UP),
                outcome.getXa().subtract(prediction.getPredictedXa()).setScale(2, RoundingMode.HALF_UP),
                Instant.now()
        );
        prediction.attachEvaluation(evaluation);
        predictionEvaluationRepository.save(evaluation);
        return predictionMapper.toResponse(prediction);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        findDetailed(id).softDelete();
    }

    private Prediction findDetailed(UUID id) {
        return predictionRepository.findDetailedById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Prediction", id));
    }

    private static BigDecimal diff(int actual, BigDecimal predicted) {
        return BigDecimal.valueOf(actual).subtract(predicted).setScale(2, RoundingMode.HALF_UP);
    }
}
