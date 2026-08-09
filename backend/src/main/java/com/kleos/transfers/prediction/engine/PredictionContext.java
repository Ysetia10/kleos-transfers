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
 *
 * @param targetClubSquad        prior-season roster of the target club, as recorded
 * @param departingSquadMembers  prior-season rows of players leaving in the target window
 * @param arrivingSquadMembers   previous-club rows of the other players arriving in the same window
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
        Optional<String> targetManagerName,
        List<PlayerSeason> departingSquadMembers,
        List<PlayerSeason> arrivingSquadMembers
) {

    public PredictionContext(
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
        this(
                player,
                targetClub,
                season,
                playerHistory,
                targetClubSquad,
                recentInjuries,
                playerContracts,
                targetClubSeason,
                mostRecentSeason,
                targetManagerName,
                List.of(),
                List.of()
        );
    }

    public int ageAtSeasonStart() {
        return Period.between(player.getDateOfBirth(), season.getStartDate()).getYears();
    }

    public LocalDate injuryLookbackStart() {
        return season.getStartDate().minusYears(1);
    }
}
