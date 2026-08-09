package com.kleos.transfers.prediction.mapper;

import com.kleos.transfers.prediction.dto.CompatibilityBreakdownResponse;
import com.kleos.transfers.prediction.dto.EvaluationResponse;
import com.kleos.transfers.prediction.dto.ExplanationResponse;
import com.kleos.transfers.prediction.dto.PredictionResponse;
import com.kleos.transfers.prediction.dto.PredictionRunResponse;
import com.kleos.transfers.prediction.engine.CompatibilityBreakdown;
import com.kleos.transfers.prediction.engine.EngineResult;
import com.kleos.transfers.prediction.engine.ExplanationFactor;
import com.kleos.transfers.prediction.entity.Prediction;
import com.kleos.transfers.prediction.entity.PredictionEvaluation;
import com.kleos.transfers.prediction.entity.PredictionExplanation;
import com.kleos.transfers.prediction.entity.PredictionRun;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps prediction persistence models to API contracts.
 */
@Component
public class PredictionMapper {

    public Prediction toEntity(PredictionRun run, EngineResult result, PredictionContextRefs refs) {
        CompatibilityBreakdown breakdown = result.compatibilityBreakdown();
        return new Prediction(
                run,
                refs.player(),
                refs.targetClub(),
                refs.season(),
                result.predictedMinutes(),
                result.predictedGoals(),
                result.predictedAssists(),
                result.predictedXg(),
                result.predictedXa(),
                result.predictedMarketValueEur(),
                result.compatibilityScore(),
                breakdown == null ? null : breakdown.system(),
                breakdown == null ? null : breakdown.role(),
                breakdown == null ? null : breakdown.tempo(),
                breakdown == null ? null : breakdown.league(),
                breakdown == null ? null : breakdown.manager(),
                result.confidenceScore()
        );
    }

    public void attachExplanations(Prediction prediction, List<ExplanationFactor> factors) {
        int order = 0;
        for (ExplanationFactor factor : factors) {
            prediction.addExplanation(new PredictionExplanation(
                    prediction,
                    factor.code(),
                    factor.label(),
                    factor.direction(),
                    factor.impact(),
                    factor.detail(),
                    order++
            ));
        }
    }

    public PredictionResponse toResponse(Prediction prediction) {
        PredictionRun run = prediction.getRun();
        return new PredictionResponse(
                prediction.getId(),
                run.getId(),
                run.getModelVersion(),
                prediction.getPlayer().getId(),
                prediction.getPlayer().getFullName(),
                prediction.getTargetClub().getId(),
                prediction.getTargetClub().getName(),
                prediction.getSeason().getId(),
                prediction.getSeason().getLabel(),
                prediction.getPredictedMinutes(),
                prediction.getPredictedGoals(),
                prediction.getPredictedAssists(),
                prediction.getPredictedXg(),
                prediction.getPredictedXa(),
                prediction.getPredictedMarketValueEur(),
                prediction.getCompatibilityScore(),
                toBreakdown(prediction),
                prediction.getConfidenceScore(),
                prediction.getExplanations().stream().map(this::toExplanation).toList(),
                prediction.getEvaluation() == null ? null : toEvaluation(prediction.getEvaluation()),
                prediction.getCreatedAt(),
                prediction.getUpdatedAt()
        );
    }

    private CompatibilityBreakdownResponse toBreakdown(Prediction prediction) {
        if (prediction.getCompatibilitySystem() == null
                || prediction.getCompatibilityRole() == null
                || prediction.getCompatibilityTempo() == null
                || prediction.getCompatibilityLeague() == null
                || prediction.getCompatibilityManager() == null) {
            return null;
        }
        return new CompatibilityBreakdownResponse(
                prediction.getCompatibilitySystem(),
                prediction.getCompatibilityRole(),
                prediction.getCompatibilityTempo(),
                prediction.getCompatibilityLeague(),
                prediction.getCompatibilityManager()
        );
    }

    public PredictionRunResponse toRunResponse(PredictionRun run, List<Prediction> predictions) {
        return new PredictionRunResponse(
                run.getId(),
                run.getModelVersion(),
                run.getNote(),
                run.getCreatedAt(),
                predictions.stream().map(this::toResponse).toList()
        );
    }

    public ExplanationResponse toExplanation(PredictionExplanation explanation) {
        return new ExplanationResponse(
                explanation.getId(),
                explanation.getFactorCode(),
                explanation.getLabel(),
                explanation.getDirection(),
                explanation.getImpact(),
                explanation.getDetail(),
                explanation.getSortOrder()
        );
    }

    public EvaluationResponse toEvaluation(PredictionEvaluation evaluation) {
        return new EvaluationResponse(
                evaluation.getId(),
                evaluation.getActualMinutes(),
                evaluation.getActualGoals(),
                evaluation.getActualAssists(),
                evaluation.getActualXg(),
                evaluation.getActualXa(),
                evaluation.getMinutesError(),
                evaluation.getGoalsError(),
                evaluation.getAssistsError(),
                evaluation.getXgError(),
                evaluation.getXaError(),
                evaluation.getEvaluatedAt()
        );
    }

    /**
     * Lightweight holder so the mapper does not depend on PredictionContext.
     */
    public record PredictionContextRefs(
            com.kleos.transfers.player.entity.Player player,
            com.kleos.transfers.club.entity.Club targetClub,
            com.kleos.transfers.season.entity.Season season
    ) {
    }
}
