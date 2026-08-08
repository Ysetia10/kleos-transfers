package com.kleos.transfers.prediction.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.domain.InjurySeverity;
import com.kleos.transfers.domain.DateOfBirthPrecision;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import com.kleos.transfers.injury.entity.Injury;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.season.entity.Season;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MinutesPredictorTest {

    private final MinutesPredictor predictor = new MinutesPredictor();

    @Test
    void usesDefaultBaselineWhenHistoryMissing() {
        MinutesPredictor.Result result = predictor.predict(baseContext(
                player(LocalDate.of(2001, 1, 1)),
                List.of(),
                List.of(),
                List.of()
        ));

        assertThat(result.minutes()).isBetween(1_400, 2_200);
        assertThat(result.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.RECENT_MINUTES, FactorCodes.AGE_PROFILE);
    }

    @Test
    void reducesMinutesForSevereRecentInjuryAndBusyPositionGroup() {
        Player player = player(LocalDate.of(2000, 6, 15));
        Club priorClub = club("Dortmund", "GER");
        Club target = club("Real Madrid", "ESP");
        Season priorSeason = season("2023/24", LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30));
        Season targetSeason = season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));

        PlayerSeason history = new PlayerSeason(
                player,
                priorClub,
                priorSeason,
                34,
                3_000,
                12,
                8,
                new BigDecimal("10.5"),
                new BigDecimal("7.2"),
                Position.CM
        );

        MinutesPredictor.Result healthy = predictor.predict(new PredictionContext(
                player,
                target,
                targetSeason,
                List.of(history),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history)
        ));

        Player rival = player(LocalDate.of(1998, 1, 1));
        PlayerSeason squadMate = new PlayerSeason(
                rival,
                target,
                targetSeason,
                30,
                2_500,
                5,
                5,
                new BigDecimal("4.0"),
                new BigDecimal("4.0"),
                Position.CM
        );
        Injury injury = new Injury(
                player,
                "ACL rupture",
                InjurySeverity.SEVERE,
                LocalDate.of(2024, 3, 1),
                null
        );

        MinutesPredictor.Result injured = predictor.predict(new PredictionContext(
                player,
                target,
                targetSeason,
                List.of(history),
                List.of(squadMate),
                List.of(injury),
                List.of(),
                Optional.empty(),
                Optional.of(history)
        ));

        assertThat(injured.minutes()).isLessThan(healthy.minutes());
        assertThat(injured.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.INJURY_BURDEN, FactorCodes.SQUAD_COMPETITION);
    }

    @Test
    void softensCompetitionHaircutForEstablishedStarters() {
        Player player = player(LocalDate.of(1999, 4, 1));
        Club priorClub = club("Ajax", "NED");
        Club target = club("Arsenal", "ENG");
        Season priorSeason = season("2023/24", LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30));
        Season targetSeason = season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));

        PlayerSeason history = new PlayerSeason(
                player,
                priorClub,
                priorSeason,
                34,
                3_100,
                8,
                6,
                new BigDecimal("7.0"),
                new BigDecimal("5.0"),
                Position.CM
        );

        List<PlayerSeason> crowdedSquad = List.of(
                squadMate(target, targetSeason, Position.CM),
                squadMate(target, targetSeason, Position.CM),
                squadMate(target, targetSeason, Position.CM),
                squadMate(target, targetSeason, Position.CM)
        );

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                player,
                target,
                targetSeason,
                List.of(history),
                crowdedSquad,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history)
        ));

        // v0 would apply 0.70 competition on 4 rivals (~2170); v0.1 should stay higher.
        assertThat(result.minutes()).isGreaterThan(2_400);
    }

    @Test
    void keepsHighMinutesForStarterGoalkeeperDespiteMultipleListedGks() {
        Player keeper = player(LocalDate.of(1995, 3, 1), Position.GK);
        Club priorClub = club("Brighton", "ENG");
        Club target = club("Chelsea", "ENG");
        Season priorSeason = season("2023/24", LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30));
        Season targetSeason = season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));

        PlayerSeason history = new PlayerSeason(
                keeper,
                priorClub,
                priorSeason,
                38,
                3_420,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        List<PlayerSeason> squad = List.of(
                gkSquadMate(target, priorSeason, 180),
                gkSquadMate(target, priorSeason, 90),
                gkSquadMate(target, priorSeason, 0)
        );

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                keeper,
                target,
                targetSeason,
                List.of(history),
                squad,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history)
        ));

        assertThat(result.minutes()).isGreaterThan(3_000);
        assertThat(result.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.GK_ROLE);
    }

    @Test
    void keepsBackupGoalkeeperMinutesLowWhenClubHasStarter() {
        Player keeper = player(LocalDate.of(1998, 8, 12), Position.GK);
        Club priorClub = club("Burnley", "ENG");
        Club target = club("Arsenal", "ENG");
        Season priorSeason = season("2023/24", LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30));
        Season targetSeason = season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));

        PlayerSeason history = new PlayerSeason(
                keeper,
                priorClub,
                priorSeason,
                4,
                360,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        List<PlayerSeason> squad = List.of(gkSquadMate(target, priorSeason, 3_420));

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                keeper,
                target,
                targetSeason,
                List.of(history),
                squad,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history)
        ));

        assertThat(result.minutes()).isLessThan(800);
        assertThat(result.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.GK_ROLE, FactorCodes.SQUAD_COMPETITION);
    }

    @Test
    void clampsToSeasonMaximum() {
        Player player = player(LocalDate.of(2002, 1, 1));
        Club club = club("Arsenal", "ENG");
        Season season = season("2023/24", LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30));
        PlayerSeason history = new PlayerSeason(
                player,
                club,
                season,
                38,
                5_000,
                20,
                10,
                new BigDecimal("18"),
                new BigDecimal("9"),
                Position.ST
        );

        MinutesPredictor.Result result = predictor.predict(baseContext(
                player,
                List.of(history),
                List.of(),
                List.of()
        ));

        assertThat(result.minutes()).isLessThanOrEqualTo(MinutesPredictor.MAX_MINUTES);
    }

    private PredictionContext baseContext(
            Player player,
            List<PlayerSeason> history,
            List<PlayerSeason> squad,
            List<Injury> injuries
    ) {
        Season season = season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));
        return new PredictionContext(
                player,
                club("Arsenal", "ENG"),
                season,
                history,
                squad,
                injuries,
                List.of(),
                Optional.empty(),
                history.stream().findFirst()
        );
    }

    private PlayerSeason squadMate(Club club, Season season, Position position) {
        Player rival = player(LocalDate.of(1997, 1, 1), position);
        return new PlayerSeason(
                rival,
                club,
                season,
                28,
                2_200,
                3,
                2,
                new BigDecimal("2.5"),
                new BigDecimal("2.0"),
                position
        );
    }

    private PlayerSeason gkSquadMate(Club club, Season season, int minutes) {
        Player rival = player(LocalDate.of(1994, 5, 5), Position.GK);
        return new PlayerSeason(
                rival,
                club,
                season,
                Math.max(1, minutes / 90),
                minutes,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );
    }

    private Player player(LocalDate dob) {
        return player(dob, Position.CM);
    }

    private Player player(LocalDate dob, Position position) {
        Player player = new Player(
                "Test Player",
                dob,
                DateOfBirthPrecision.DAY,
                "ENG",
                180,
                PreferredFoot.RIGHT,
                position
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
