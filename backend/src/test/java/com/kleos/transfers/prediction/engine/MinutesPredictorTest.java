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
                Optional.of(history),
                Optional.empty()
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
                Optional.of(history),
                Optional.empty()
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
                Optional.of(history),
                Optional.empty()
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
                Optional.of(history),
                Optional.empty()
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
                Optional.of(history),
                Optional.empty()
        ));

        assertThat(result.minutes()).isLessThan(800);
        assertThat(result.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.GK_ROLE, FactorCodes.SQUAD_COMPETITION);
    }

    @Test
    void judgesFullBackCompetitionByFlankRatherThanBackLine() {
        Club target = club("Newcastle", "ENG");
        Season priorSeason = season("2023/24", LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30));

        List<PlayerSeason> leftSidedDefence = List.of(
                squadMate(target, priorSeason, Position.LB, 3_000),
                squadMate(target, priorSeason, Position.LB, 2_400),
                squadMate(target, priorSeason, Position.CB, 3_000),
                squadMate(target, priorSeason, Position.CB, 2_800),
                squadMate(target, priorSeason, Position.CM, 2_800)
        );
        List<PlayerSeason> defenceWithRightBack = List.of(
                squadMate(target, priorSeason, Position.RB, 3_000),
                squadMate(target, priorSeason, Position.LB, 3_000),
                squadMate(target, priorSeason, Position.CB, 3_000),
                squadMate(target, priorSeason, Position.CM, 2_800)
        );

        MinutesPredictor.Result openFlank = predictRightBack(target, leftSidedDefence);
        MinutesPredictor.Result blockedFlank = predictRightBack(target, defenceWithRightBack);

        // Two left-backs and a stack of centre-backs do not contest the right-back slot.
        assertThat(openFlank.minutes()).isGreaterThan(2_600);
        assertThat(blockedFlank.minutes()).isLessThan(openFlank.minutes() - 500);
    }

    @Test
    void treatsDepartingStarterAsAVacatedSlotForTheReplacement() {
        Club target = club("Liverpool", "ENG");
        Season priorSeason = season("2025/26", LocalDate.of(2025, 7, 1), LocalDate.of(2026, 6, 30));
        Season targetSeason = season("2026/27", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));

        PlayerSeason departingStarter = squadMate(target, priorSeason, Position.CB, 2_900);
        List<PlayerSeason> squad = List.of(
                departingStarter,
                squadMate(target, priorSeason, Position.CB, 2_600),
                squadMate(target, priorSeason, Position.LB, 2_800),
                squadMate(target, priorSeason, Position.RB, 2_700),
                squadMate(target, priorSeason, Position.CM, 3_000)
        );

        Player signing = player(LocalDate.of(1999, 5, 20), Position.CB);
        PlayerSeason history = new PlayerSeason(
                signing,
                club("Bournemouth", "ENG"),
                priorSeason,
                30,
                2_400,
                2,
                1,
                new BigDecimal("1.5"),
                new BigDecimal("1.0"),
                Position.CB
        );

        MinutesPredictor.Result incumbentStays = predictor.predict(new PredictionContext(
                signing,
                target,
                targetSeason,
                List.of(history),
                squad,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        MinutesPredictor.Result replacesStarter = predictor.predict(new PredictionContext(
                signing,
                target,
                targetSeason,
                List.of(history),
                squad,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty(),
                List.of(departingStarter),
                List.of()
        ));

        assertThat(replacesStarter.minutes()).isGreaterThan(incumbentStays.minutes());
        assertThat(replacesStarter.minutes()).isGreaterThan(2_400);
        assertThat(replacesStarter.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.SQUAD_VACANCY);
    }

    @Test
    void floorsWalkInMinutesWhenStarterSlotIsVacated() {
        Club target = club("Crotone", "ITA");
        Season priorSeason = season("2016/17", LocalDate.of(2016, 7, 1), LocalDate.of(2017, 6, 30));
        Season targetSeason = season("2017/18", LocalDate.of(2017, 7, 1), LocalDate.of(2018, 6, 30));

        PlayerSeason departingStarter = squadMate(target, priorSeason, Position.CM, 2_800);
        List<PlayerSeason> squad = List.of(
                departingStarter,
                squadMate(target, priorSeason, Position.CM, 900),
                squadMate(target, priorSeason, Position.CB, 2_500)
        );

        Player signing = player(LocalDate.of(1997, 6, 1), Position.CM);
        PlayerSeason history = new PlayerSeason(
                signing,
                club("Juventus", "ITA"),
                priorSeason,
                1,
                5,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.CM
        );

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                signing,
                target,
                targetSeason,
                List.of(history),
                squad,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty(),
                List.of(departingStarter),
                List.of()
        ));

        assertThat(result.minutes()).isGreaterThanOrEqualTo(1_400);
        assertThat(result.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.SQUAD_VACANCY);
    }

    @Test
    void floorsWalkInMinutesWhenDepthIsOpenWithoutTaggedDeparture() {
        Club target = club("Como", "ITA");
        Season priorSeason = season("2023/24", LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30));
        Season targetSeason = season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));

        // Only soft rivals remain — no locked starter, no departure list.
        List<PlayerSeason> squad = List.of(
                squadMate(target, priorSeason, Position.CM, 900),
                squadMate(target, priorSeason, Position.CM, 600),
                squadMate(target, priorSeason, Position.CB, 2_400)
        );

        Player signing = player(LocalDate.of(2004, 9, 8), Position.CM);
        PlayerSeason history = new PlayerSeason(
                signing,
                club("Real Madrid", "ESP"),
                priorSeason,
                2,
                80,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.CM
        );

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                signing,
                target,
                targetSeason,
                List.of(history),
                squad,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        assertThat(result.minutes()).isGreaterThanOrEqualTo(1_500);
        assertThat(result.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.SQUAD_VACANCY);
    }

    @Test
    void ignoresFullyRecoveredInjuriesBeforeSeasonStart() {
        Player player = player(LocalDate.of(1998, 3, 1), Position.CB);
        Club priorClub = club("Liverpool", "ENG");
        Club target = club("Liverpool", "ENG");
        Season priorSeason = season("2020/21", LocalDate.of(2020, 7, 1), LocalDate.of(2021, 6, 30));
        Season targetSeason = season("2021/22", LocalDate.of(2021, 7, 1), LocalDate.of(2022, 6, 30));

        PlayerSeason history = new PlayerSeason(
                player,
                priorClub,
                priorSeason,
                5,
                370,
                1,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.CB
        );
        Injury recovered = new Injury(
                player,
                "ACL rupture",
                InjurySeverity.SEVERE,
                LocalDate.of(2020, 10, 17),
                LocalDate.of(2021, 2, 25)
        );

        MinutesPredictor.Result withRecovered = predictor.predict(new PredictionContext(
                player,
                target,
                targetSeason,
                List.of(history),
                List.of(),
                List.of(recovered),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));
        MinutesPredictor.Result healthy = predictor.predict(new PredictionContext(
                player,
                target,
                targetSeason,
                List.of(history),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        assertThat(withRecovered.minutes()).isEqualTo(healthy.minutes());
    }

    @Test
    void benchesArrivingKeeperWhenTheNumberOneStays() {
        Club target = club("Chelsea", "ENG");
        Season priorSeason = season("2025/26", LocalDate.of(2025, 7, 1), LocalDate.of(2026, 6, 30));
        Season targetSeason = season("2026/27", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));

        Player keeper = player(LocalDate.of(1997, 3, 7), Position.GK);
        PlayerSeason history = new PlayerSeason(
                keeper,
                club("Crystal Palace", "ENG"),
                priorSeason,
                27,
                2_400,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        MinutesPredictor.Result behindNumberOne = predictor.predict(new PredictionContext(
                keeper,
                target,
                targetSeason,
                List.of(history),
                List.of(gkSquadMate(target, priorSeason, 3_100), gkSquadMate(target, priorSeason, 320)),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        MinutesPredictor.Result openGoal = predictor.predict(new PredictionContext(
                keeper,
                target,
                targetSeason,
                List.of(history),
                List.of(gkSquadMate(target, priorSeason, 400)),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        assertThat(behindNumberOne.minutes()).isLessThan(800);
        assertThat(openGoal.minutes()).isGreaterThan(2_800);
    }

    @Test
    void contestsEliteKeeperPairProjectsStarterShareNotBackup() {
        Club target = club("Arsenal", "ENG");
        Season priorSeason = season("2017/18", LocalDate.of(2017, 7, 1), LocalDate.of(2018, 6, 30));
        Season targetSeason = season("2018/19", LocalDate.of(2018, 7, 1), LocalDate.of(2019, 6, 30));

        Player keeper = player(LocalDate.of(1992, 3, 20), Position.GK);
        PlayerSeason history = new PlayerSeason(
                keeper,
                club("Leverkusen", "GER"),
                priorSeason,
                34,
                2_970,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                keeper,
                target,
                targetSeason,
                List.of(history),
                List.of(gkSquadMate(target, priorSeason, 3_039)),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        // Leno/Cech pattern: both ~3k minutes, narrow edge — not a ~500' backup projection.
        assertThat(result.minutes()).isGreaterThan(2_400);
        assertThat(result.minutes()).isLessThan(3_200);
    }

    @Test
    void capsVeteranIntoVacatedShirtBelowFullStarterLock() {
        Club target = club("Roma", "ITA");
        Season priorSeason = season("2017/18", LocalDate.of(2017, 7, 1), LocalDate.of(2018, 6, 30));
        Season targetSeason = season("2018/19", LocalDate.of(2018, 7, 1), LocalDate.of(2019, 6, 30));

        Player veteran = player(LocalDate.of(1982, 7, 8), Position.GK);
        PlayerSeason history = new PlayerSeason(
                veteran,
                club("Benevento", "ITA"),
                priorSeason,
                38,
                2_970,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        PlayerSeason departingStarter = gkSquadMate(target, priorSeason, 3_330);
        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                veteran,
                target,
                targetSeason,
                List.of(history),
                List.of(departingStarter, gkSquadMate(target, priorSeason, 90)),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty(),
                List.of(departingStarter),
                List.of()
        ));

        assertThat(result.minutes()).isLessThan(2_800);
        assertThat(result.minutes()).isGreaterThan(1_800);
    }

    @Test
    void givesStarterMinutesWhenPriorNumberOneDepartsEvenIfBackupRemains() {
        Club target = club("Arsenal", "ENG");
        Season priorSeason = season("2025/26", LocalDate.of(2025, 7, 1), LocalDate.of(2026, 6, 30));
        Season targetSeason = season("2026/27", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));

        Player arrival = player(LocalDate.of(1995, 9, 15), Position.GK);
        PlayerSeason history = new PlayerSeason(
                arrival,
                club("Brentford", "ENG"),
                priorSeason,
                38,
                3_420,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        PlayerSeason departingStarter = gkSquadMate(target, priorSeason, 3_400);
        PlayerSeason residualBackup = gkSquadMate(target, priorSeason, 2_100);

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                arrival,
                target,
                targetSeason,
                List.of(history),
                List.of(departingStarter, residualBackup),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty(),
                List.of(departingStarter),
                List.of()
        ));

        assertThat(result.minutes()).isGreaterThan(2_800);
        assertThat(result.factors()).extracting(ExplanationFactor::code)
                .contains(FactorCodes.SQUAD_COMPETITION);
    }

    @Test
    void projectsFullStarterWhenEliteNumberOneVacatesDespiteThinPrior() {
        Club target = club("Bournemouth", "ENG");
        Season priorSeason = season("2016/17", LocalDate.of(2016, 7, 1), LocalDate.of(2017, 6, 30));
        Season targetSeason = season("2017/18", LocalDate.of(2017, 7, 1), LocalDate.of(2018, 6, 30));

        Player arrival = player(LocalDate.of(1987, 6, 20), Position.GK);
        PlayerSeason history = new PlayerSeason(
                arrival,
                club("Chelsea", "ENG"),
                priorSeason,
                2,
                180,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        PlayerSeason departingStarter = gkSquadMate(target, priorSeason, 3_150, LocalDate.of(1980, 2, 20));
        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                arrival,
                target,
                targetSeason,
                List.of(history),
                List.of(departingStarter, gkSquadMate(target, priorSeason, 180)),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty(),
                List.of(departingStarter),
                List.of()
        ));

        assertThat(result.minutes()).isGreaterThan(3_000);
    }

    @Test
    void projectsStarterWhenNoPriorGkDataAtPromotedClub() {
        Club target = club("Brighton", "ENG");
        Season priorSeason = season("2016/17", LocalDate.of(2016, 7, 1), LocalDate.of(2017, 6, 30));
        Season targetSeason = season("2017/18", LocalDate.of(2017, 7, 1), LocalDate.of(2018, 6, 30));

        Player arrival = player(LocalDate.of(1992, 4, 8), Position.GK);
        PlayerSeason history = new PlayerSeason(
                arrival,
                club("Valencia", "ESP"),
                priorSeason,
                2,
                180,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                arrival,
                target,
                targetSeason,
                List.of(history),
                List.of(),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        assertThat(result.minutes()).isGreaterThan(3_000);
    }

    @Test
    void splitsSeasonWhenIncumbentLoggedReducedStarterMinutes() {
        Club target = club("Juventus", "ITA");
        Season priorSeason = season("2016/17", LocalDate.of(2016, 7, 1), LocalDate.of(2017, 6, 30));
        Season targetSeason = season("2017/18", LocalDate.of(2017, 7, 1), LocalDate.of(2018, 6, 30));

        Player arrival = player(LocalDate.of(1990, 4, 18), Position.GK);
        PlayerSeason history = new PlayerSeason(
                arrival,
                club("Arsenal", "ENG"),
                priorSeason,
                38,
                3_420,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                arrival,
                target,
                targetSeason,
                List.of(history),
                List.of(gkSquadMate(target, priorSeason, 1_862, LocalDate.of(1977, 9, 28))),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        assertThat(result.minutes()).isBetween(1_400, 1_800);
        assertThat(result.minutes()).isLessThan(2_500);
    }

    @Test
    void displacesAgingIncumbentForPrimeArrival() {
        Club target = club("Marseille", "FRA");
        Season priorSeason = season("2016/17", LocalDate.of(2016, 7, 1), LocalDate.of(2017, 6, 30));
        Season targetSeason = season("2017/18", LocalDate.of(2017, 7, 1), LocalDate.of(2018, 6, 30));

        Player arrival = player(LocalDate.of(1985, 3, 28), Position.GK);
        PlayerSeason history = new PlayerSeason(
                arrival,
                club("Crystal Palace", "ENG"),
                priorSeason,
                9,
                810,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                arrival,
                target,
                targetSeason,
                List.of(history),
                List.of(gkSquadMate(target, priorSeason, 3_420, LocalDate.of(1982, 11, 4))),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        assertThat(result.minutes()).isBetween(2_500, 2_950);
    }

    @Test
    void capsTakeoverWhenAgingLegendIncumbentStillLoggedHeavyMinutes() {
        Club target = club("Juventus", "ITA");
        Season priorSeason = season("2016/17", LocalDate.of(2016, 7, 1), LocalDate.of(2017, 6, 30));
        Season targetSeason = season("2017/18", LocalDate.of(2017, 7, 1), LocalDate.of(2018, 6, 30));

        Player arrival = player(LocalDate.of(1990, 4, 18), Position.GK);
        PlayerSeason history = new PlayerSeason(
                arrival,
                club("Arsenal", "ENG"),
                priorSeason,
                38,
                3_420,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Position.GK
        );

        MinutesPredictor.Result result = predictor.predict(new PredictionContext(
                arrival,
                target,
                targetSeason,
                List.of(history),
                List.of(gkSquadMate(target, priorSeason, 2_655, LocalDate.of(1977, 9, 28))),
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));

        assertThat(result.minutes()).isBetween(1_400, 1_750);
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
                history.stream().findFirst(),
                Optional.empty()
        );
    }

    private MinutesPredictor.Result predictRightBack(Club target, List<PlayerSeason> squad) {
        Season priorSeason = season("2023/24", LocalDate.of(2023, 7, 1), LocalDate.of(2024, 6, 30));
        Season targetSeason = season("2024/25", LocalDate.of(2024, 7, 1), LocalDate.of(2025, 6, 30));
        Player fullBack = player(LocalDate.of(1999, 2, 10), Position.RB);
        PlayerSeason history = new PlayerSeason(
                fullBack,
                club("Girona", "ESP"),
                priorSeason,
                32,
                2_600,
                2,
                6,
                new BigDecimal("1.0"),
                new BigDecimal("4.5"),
                Position.RB
        );
        return predictor.predict(new PredictionContext(
                fullBack,
                target,
                targetSeason,
                List.of(history),
                squad,
                List.of(),
                List.of(),
                Optional.empty(),
                Optional.of(history),
                Optional.empty()
        ));
    }

    private PlayerSeason squadMate(Club club, Season season, Position position) {
        return squadMate(club, season, position, 2_200);
    }

    private PlayerSeason squadMate(Club club, Season season, Position position, int minutes) {
        Player rival = player(LocalDate.of(1997, 1, 1), position);
        return new PlayerSeason(
                rival,
                club,
                season,
                Math.max(1, minutes / 90),
                minutes,
                3,
                2,
                new BigDecimal("2.5"),
                new BigDecimal("2.0"),
                position
        );
    }

    private PlayerSeason gkSquadMate(Club club, Season season, int minutes) {
        return gkSquadMate(club, season, minutes, LocalDate.of(1994, 5, 5));
    }

    private PlayerSeason gkSquadMate(Club club, Season season, int minutes, LocalDate dob) {
        Player rival = player(dob, Position.GK);
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
