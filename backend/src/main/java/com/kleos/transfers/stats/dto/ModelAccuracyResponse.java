package com.kleos.transfers.stats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Published backtest accuracy for the product USP surface.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelAccuracyResponse(
        String generatedAt,
        List<String> seasons,
        String modelVersion,
        Selection selection,
        MetricBlock metrics,
        Map<String, LeagueAccuracy> byLeague,
        List<SamplePrediction> samplePredictions
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Selection(
            int minMinutes,
            boolean requireClubChange,
            int perLeagueLimit,
            List<String> countries,
            List<String> seasons,
            int candidatesFound,
            int evaluated,
            int failed
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetricTriple(
            double mae,
            double rmse,
            @JsonProperty("bias_actual_minus_predicted") double biasActualMinusPredicted
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetricBlock(
            int n,
            MetricTriple minutes,
            MetricTriple goals,
            MetricTriple assists
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LeagueAccuracy(
            String countryCode,
            String leagueName,
            MetricBlock metrics,
            List<SamplePrediction> samples
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SamplePrediction(
            String player,
            String playerId,
            String position,
            String club,
            String clubId,
            String season,
            String league,
            String countryCode,
            int predictedMinutes,
            int actualMinutes,
            Number minutesError,
            Number predictedGoals,
            Number actualGoals,
            Number goalsError,
            Number predictedAssists,
            Number actualAssists,
            Number assistsError,
            String predictionId
    ) {
    }
}
