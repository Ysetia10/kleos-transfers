package com.kleos.transfers.prediction.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.domain.DateOfBirthPrecision;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.prediction.entity.PredictionRun;
import com.kleos.transfers.season.entity.Season;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeuristicPredictionEngineTest {

    private HeuristicPredictionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new HeuristicPredictionEngine(
                new MinutesPredictor(),
                new OutputPredictors(),
                new MarketValuePredictor(),
                new CompatibilityScorer(),
                new ConfidenceScorer()
        );
    }

    @Test
    void producesBoundedScoresAndExplanationFactors() {
        Player player = player(LocalDate.of(2001, 5, 10));
        Club from = club("Dortmund", "GER");
        Club to = club("Bayern", "GER");
        Season prior = season("2023/24", LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30));
        Season target = season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));
        PlayerSeason history = new PlayerSeason(
                player,
                from,
                prior,
                32,
                2_700,
                10,
                7,
                new BigDecimal("9.5"),
                new BigDecimal("6.1"),
                Position.CM
        );

        PredictionContext context = new PredictionContext(
                player,
                to,
                target,
                List.of(history),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.of("Thomas Tuchel")
        );

        EngineResult result = engine.predict(context);

        assertThat(engine.modelVersion()).isEqualTo(PredictionRun.MODEL_VERSION_V0_12);
        assertThat(result.predictedMinutes()).isBetween(0, MinutesPredictor.MAX_MINUTES);
        assertThat(result.predictedMinutesLow()).isBetween(0, result.predictedMinutes());
        assertThat(result.predictedMinutesHigh()).isBetween(result.predictedMinutes(), MinutesPredictor.MAX_MINUTES);
        assertThat(result.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.MINUTES_INTERVAL);
        assertThat(result.predictedGoals()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(result.predictedAssists()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(result.predictedMarketValueEur()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.compatibilityScore()).isBetween(BigDecimal.ZERO, PredictionMath.bd(100));
        assertThat(result.compatibilityBreakdown()).isNotNull();
        assertThat(result.compatibilityBreakdown().system()).isBetween(BigDecimal.ZERO, PredictionMath.bd(100));
        assertThat(result.compatibilityBreakdown().role()).isBetween(BigDecimal.ZERO, PredictionMath.bd(100));
        assertThat(result.compatibilityBreakdown().tempo()).isBetween(BigDecimal.ZERO, PredictionMath.bd(100));
        assertThat(result.compatibilityBreakdown().league()).isBetween(BigDecimal.ZERO, PredictionMath.bd(100));
        assertThat(result.compatibilityBreakdown().manager()).isBetween(BigDecimal.ZERO, PredictionMath.bd(100));
        assertThat(result.confidenceScore()).isBetween(BigDecimal.ZERO, PredictionMath.bd(100));
        assertThat(result.factors()).isNotEmpty();
        assertThat(result.factors()).extracting(ExplanationFactor::code)
                .contains(
                        FactorCodes.RECENT_MINUTES,
                        FactorCodes.SCORING_RATE,
                        FactorCodes.CREATION_RATE,
                        FactorCodes.PERFORMANCE_VALUE,
                        FactorCodes.DATA_COVERAGE,
                        FactorCodes.MANAGER_CONTEXT
                );
    }

    private Player player(LocalDate dob) {
        Player player = new Player(
                "Jude Bellingham",
                dob,
                DateOfBirthPrecision.DAY,
                "ENG",
                186,
                PreferredFoot.RIGHT,
                Position.CM
        );
        setId(player, UUID.randomUUID());
        return player;
    }

    private Club club(String name, String country) {
        Club club = new Club(name, name.substring(0, 3).toUpperCase(), country, 1900);
        setId(club, UUID.randomUUID());
        return club;
    }

    private Season season(String label, LocalDate start, LocalDate end) {
        Season season = new Season(label, start, end);
        setId(season, UUID.randomUUID());
        return season;
    }

    private static void setId(Object entity, UUID id) {
        try {
            var idField = entity.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
