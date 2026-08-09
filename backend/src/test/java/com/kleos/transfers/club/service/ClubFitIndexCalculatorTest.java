package com.kleos.transfers.club.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kleos.transfers.domain.RecruitmentSignal;
import com.kleos.transfers.domain.TacticalSystem;
import com.kleos.transfers.domain.TempoProfile;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ClubFitIndexCalculatorTest {

    @Test
    void scoresAbsoluteClubEnvironmentNotPlayerRelative() {
        ClubFitIndexCalculator.Result rich = ClubFitIndexCalculator.compute(
                new ClubFitIndexCalculator.Input(
                        true,
                        TacticalSystem.POSSESSION,
                        TempoProfile.HIGH,
                        new BigDecimal("28.0"),
                        "ENG",
                        true
                )
        );
        ClubFitIndexCalculator.Result thin = ClubFitIndexCalculator.compute(
                new ClubFitIndexCalculator.Input(false, null, null, null, "USA", false)
        );

        assertThat(rich.version()).isEqualTo(ClubFitIndexCalculator.VERSION);
        assertThat(rich.fitIndex()).isGreaterThan(thin.fitIndex());
        assertThat(rich.recruitmentSignal()).isEqualTo(RecruitmentSignal.HIGH);
        assertThat(thin.recruitmentSignal()).isEqualTo(RecruitmentSignal.UNKNOWN);
    }
}
