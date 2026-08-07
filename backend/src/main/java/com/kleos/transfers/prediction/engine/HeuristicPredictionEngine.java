package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.prediction.entity.PredictionRun;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Deterministic v0 engine: minutes → counting/expected stats → value → scores.
 *
 * <p>Each step contributes explanation factors; nothing is a black-box-only number.
 */
@Component
@RequiredArgsConstructor
public class HeuristicPredictionEngine implements PredictionEngine {

    private final MinutesPredictor minutesPredictor;
    private final OutputPredictors outputPredictors;
    private final MarketValuePredictor marketValuePredictor;
    private final CompatibilityScorer compatibilityScorer;
    private final ConfidenceScorer confidenceScorer;

    @Override
    public String modelVersion() {
        return PredictionRun.MODEL_VERSION_V0_2;
    }

    @Override
    public EngineResult predict(PredictionContext context) {
        List<ExplanationFactor> factors = new ArrayList<>();

        MinutesPredictor.Result minutes = minutesPredictor.predict(context);
        factors.addAll(minutes.factors());

        OutputPredictors.CountingResult goals = outputPredictors.predictGoals(context, minutes.minutes());
        factors.addAll(goals.factors());

        OutputPredictors.CountingResult assists = outputPredictors.predictAssists(context, minutes.minutes());
        factors.addAll(assists.factors());

        OutputPredictors.CountingResult xg = outputPredictors.predictXg(context, minutes.minutes());
        factors.addAll(xg.factors());

        OutputPredictors.CountingResult xa = outputPredictors.predictXa(context, minutes.minutes());
        factors.addAll(xa.factors());

        MarketValuePredictor.Result value = marketValuePredictor.predict(
                context,
                minutes.minutes(),
                goals.value(),
                assists.value()
        );
        factors.addAll(value.factors());

        CompatibilityScorer.Result compatibility = compatibilityScorer.score(context, minutes.minutes());
        factors.addAll(compatibility.factors());

        ConfidenceScorer.Result confidence = confidenceScorer.score(context);
        factors.addAll(confidence.factors());

        return new EngineResult(
                minutes.minutes(),
                goals.value(),
                assists.value(),
                xg.value(),
                xa.value(),
                value.valueEur(),
                compatibility.score(),
                confidence.score(),
                List.copyOf(factors)
        );
    }
}
