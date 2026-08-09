package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.clubseason.entity.ClubSeason;
import com.kleos.transfers.contract.entity.Contract;
import com.kleos.transfers.injury.entity.Injury;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.season.entity.Season;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

/**
 * Immutable snapshot of historical inputs used by the v0 heuristic engine.
 */
public record PredictionContext(
        Player player,
        Club targetClub,
        Season season,
        List<PlayerSeason> playerHistory,
        List<PlayerSeason> targetClubSquad,
        List<Injury> recentInjuries,
        List<Contract> playerContracts,
        Optional<ClubSeason> targetClubSeason,
        Optional<PlayerSeason> mostRecentSeason,
        Optional<String> targetManagerName
) {

    public int ageAtSeasonStart() {
        return Period.between(player.getDateOfBirth(), season.getStartDate()).getYears();
    }

    public LocalDate injuryLookbackStart() {
        return season.getStartDate().minusYears(1);
    }
}
