package com.kleos.transfers.club.lineup;

import static org.assertj.core.api.Assertions.assertThat;

import com.kleos.transfers.club.dto.LikelyLineupResponse;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import com.kleos.transfers.transfer.dto.TransferMoveSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LikelyLineupAnalyzerTest {

    private final LikelyLineupAnalyzer analyzer = new LikelyLineupAnalyzer();

    @Test
    void picksThreeAtBackWhenCentreBacksDominateAndLeftBackIsBackup() {
        List<PlayerSeasonResponse> squad = List.of(
                row("GK1", Position.GK, 3200, null),
                row("CB1", Position.CB, 3000, null),
                row("CB2", Position.CB, 2950, null),
                row("CB3", Position.CB, 2900, null),
                row("RB1", Position.RB, 2500, null),
                row("LB1", Position.LB, 500, null),
                row("CM1", Position.CM, 2800, null),
                row("CM2", Position.CM, 2700, null),
                row("CM3", Position.CM, 2600, null),
                row("LW1", Position.LW, 400, null),
                row("ST1", Position.ST, 3000, null),
                row("RW1", Position.RW, 2300, null)
        );

        LikelyLineupResponse lineup = analyzer.analyze(squad);

        assertThat(lineup.rolePrecisionAvailable()).isTrue();
        assertThat(lineup.formation()).startsWith("3-");
        assertThat(lineup.placements()).hasSize(11);
        assertThat(lineup.placements().stream().anyMatch(p -> p.player().playerName().equals("RB1")))
                .isTrue();
    }

    @Test
    void keepsPriorElevenWhenNobodyLeavesAndIgnoresRecruitMinutesForFormation() {
        UUID gkId = UUID.randomUUID();
        UUID lbId = UUID.randomUUID();
        UUID lcbId = UUID.randomUUID();
        UUID rcbId = UUID.randomUUID();
        UUID rbId = UUID.randomUUID();
        UUID lcmId = UUID.randomUUID();
        UUID cmId = UUID.randomUUID();
        UUID rcmId = UUID.randomUUID();
        UUID lwId = UUID.randomUUID();
        UUID stId = UUID.randomUUID();
        UUID rwId = UUID.randomUUID();

        List<PlayerSeasonResponse> prior = List.of(
                row(gkId, "Raya", Position.GK, 3300, null),
                row(lbId, "Calafiori", Position.LB, 1700, null),
                row(lcbId, "Gabriel", Position.CB, 2750, null),
                row(rcbId, "Saliba", Position.CB, 2600, null),
                row(rbId, "Timber", Position.RB, 2400, null),
                row(lcmId, "Rice", Position.CM, 3100, null),
                row(cmId, "Zubimendi", Position.CDM, 2950, null),
                row(rcmId, "Odegaard", Position.CAM, 1400, null),
                row(lwId, "Saka", Position.LW, 2200, null),
                row(stId, "Gyokeres", Position.ST, 2200, null),
                row(rwId, "Madueke", Position.RW, 1200, null),
                row(UUID.randomUUID(), "Bench CB", Position.CB, 700, null)
        );

        List<PlayerSeasonResponse> projected = List.of(
                row(gkId, "Raya", Position.GK, 3300, null),
                row(lbId, "Calafiori", Position.LB, 1700, null),
                row(lcbId, "Gabriel", Position.CB, 2750, null),
                row(rcbId, "Saliba", Position.CB, 2600, null),
                row(rbId, "Timber", Position.RB, 2400, null),
                row(lcmId, "Rice", Position.CM, 3100, null),
                row(cmId, "Zubimendi", Position.CDM, 2950, null),
                row(rcmId, "Odegaard", Position.CAM, 1400, null),
                row(lwId, "Saka", Position.LW, 2200, null),
                row(stId, "Gyokeres", Position.ST, 2200, null),
                row(rwId, "Madueke", Position.RW, 1200, null),
                row(UUID.randomUUID(), "Konsa", Position.CB, 3050, arrival()),
                row(UUID.randomUUID(), "Meslier", Position.GK, 3060, arrival())
        );

        LikelyLineupResponse lineup = analyzer.analyze(prior, projected);

        assertThat(lineup.formation()).isEqualTo("4-3-3");
        assertThat(lineup.placements().stream().map(p -> p.player().playerName()))
                .containsExactly(
                        "Raya",
                        "Calafiori",
                        "Gabriel",
                        "Saliba",
                        "Timber",
                        "Rice",
                        "Zubimendi",
                        "Odegaard",
                        "Saka",
                        "Gyokeres",
                        "Madueke"
                );
    }

    @Test
    void swapsDepartedStarterForRecruitThenFallsBackToIncumbent() {
        UUID gkId = UUID.randomUUID();
        UUID cbId = UUID.randomUUID();
        UUID recruitId = UUID.randomUUID();

        List<PlayerSeasonResponse> prior = List.of(
                row(gkId, "Keeper", Position.GK, 3000, null),
                row(cbId, "Saliba", Position.CB, 2800, null),
                row(UUID.randomUUID(), "Gabriel", Position.CB, 2700, null),
                row(UUID.randomUUID(), "Calafiori", Position.LB, 1700, null),
                row(UUID.randomUUID(), "Timber", Position.RB, 1600, null),
                row(UUID.randomUUID(), "Rice", Position.CM, 3000, null),
                row(UUID.randomUUID(), "Zubimendi", Position.CDM, 2900, null),
                row(UUID.randomUUID(), "Odegaard", Position.CAM, 1500, null),
                row(UUID.randomUUID(), "Saka", Position.LW, 2200, null),
                row(UUID.randomUUID(), "Gyokeres", Position.ST, 2100, null),
                row(UUID.randomUUID(), "Madueke", Position.RW, 1200, null)
        );

        List<PlayerSeasonResponse> projected = prior.stream()
                .filter(row -> !row.playerId().equals(cbId))
                .toList();
        projected = List.of(
                projected.get(0),
                row(recruitId, "Konsa", Position.CB, 3035, arrival()),
                projected.get(1),
                projected.get(2),
                projected.get(3),
                projected.get(4),
                projected.get(5),
                projected.get(6),
                projected.get(7),
                projected.get(8),
                projected.get(9)
        );

        LikelyLineupResponse lineup = analyzer.analyze(prior, projected);

        assertThat(lineup.placements().stream().map(p -> p.player().playerName())).contains("Konsa");
        assertThat(lineup.placements().stream().map(p -> p.player().playerName())).doesNotContain("Saliba");
    }

    private static PlayerSeasonResponse row(String name, Position position, int minutes, TransferMoveSummary inbound) {
        return row(UUID.randomUUID(), name, position, minutes, inbound);
    }

    private static PlayerSeasonResponse row(
            UUID playerId,
            String name,
            Position position,
            int minutes,
            TransferMoveSummary inbound
    ) {
        return new PlayerSeasonResponse(
                UUID.randomUUID(),
                playerId,
                name,
                null,
                UUID.randomUUID(),
                "Club",
                UUID.randomUUID(),
                "2025/26",
                30,
                minutes,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                position,
                Instant.now(),
                Instant.now(),
                inbound
        );
    }

    private static TransferMoveSummary arrival() {
        return new TransferMoveSummary(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Other Club",
                UUID.randomUUID(),
                "Arsenal",
                BigDecimal.valueOf(50_000_000),
                LocalDate.of(2026, 7, 1),
                "2026/27",
                LocalDate.of(2026, 7, 1)
        );
    }
}
