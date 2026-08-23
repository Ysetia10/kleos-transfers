package com.kleos.transfers.club.lineup;

import static org.assertj.core.api.Assertions.assertThat;

import com.kleos.transfers.club.dto.LikelyLineupPlacementResponse;
import com.kleos.transfers.club.dto.LikelyLineupResponse;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.playerseason.dto.PlayerSeasonResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LikelyLineupAnalyzerTest {

    private final LikelyLineupAnalyzer analyzer = new LikelyLineupAnalyzer();

    @Test
    void picksThreeAtBackWhenCentreBacksDominateAndLeftBackIsBackup() {
        List<PlayerSeasonResponse> squad = List.of(
                row("GK1", Position.GK, 3200),
                row("CB1", Position.CB, 3000),
                row("CB2", Position.CB, 2950),
                row("CB3", Position.CB, 2900),
                row("RB1", Position.RB, 2500),
                row("LB1", Position.LB, 500),
                row("CM1", Position.CM, 2800),
                row("CM2", Position.CM, 2700),
                row("CM3", Position.CM, 2600),
                row("LW1", Position.LW, 2400),
                row("ST1", Position.ST, 3000),
                row("RW1", Position.RW, 2300)
        );

        LikelyLineupResponse lineup = analyzer.analyze(squad);

        assertThat(lineup.rolePrecisionAvailable()).isTrue();
        assertThat(lineup.formation()).isEqualTo("3-4-3");
        assertThat(lineup.placements()).hasSize(11);
        assertThat(lineup.placements().stream().map(p -> p.player().playerName()))
                .doesNotContain("LB1");
        assertThat(lineup.placements().stream().anyMatch(p -> p.player().playerName().equals("RB1")))
                .isTrue();
        assertThat(lineup.placements().stream().filter(p -> !p.likelyStarter()).count()).isZero();
    }

    private static PlayerSeasonResponse row(String name, Position position, int minutes) {
        return new PlayerSeasonResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                name,
                null,
                UUID.randomUUID(),
                "Club",
                UUID.randomUUID(),
                "2024/25",
                30,
                minutes,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                position,
                Instant.now(),
                Instant.now(),
                null
        );
    }
}
