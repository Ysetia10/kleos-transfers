package com.kleos.transfers.prediction.engine;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.club.repository.ClubRepository;
import com.kleos.transfers.clubseason.entity.ClubSeason;
import com.kleos.transfers.clubseason.repository.ClubSeasonRepository;
import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.contract.entity.Contract;
import com.kleos.transfers.contract.repository.ContractRepository;
import com.kleos.transfers.injury.entity.Injury;
import com.kleos.transfers.injury.repository.InjuryRepository;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads identity + historical inputs for one prediction scenario.
 *
 * <p>Inputs are loaded <em>as of</em> the target season start: player history and squad
 * competition never include the target season itself, so completed-season backtests do not
 * leak outcomes into the v0 heuristic.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PredictionContextLoader {

    private final PlayerRepository playerRepository;
    private final ClubRepository clubRepository;
    private final SeasonRepository seasonRepository;
    private final PlayerSeasonRepository playerSeasonRepository;
    private final ClubSeasonRepository clubSeasonRepository;
    private final InjuryRepository injuryRepository;
    private final ContractRepository contractRepository;

    public PredictionContext load(UUID playerId, UUID targetClubId, UUID seasonId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Player", playerId));
        Club targetClub = clubRepository.findById(targetClubId)
                .orElseThrow(() -> ResourceNotFoundException.of("Club", targetClubId));
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> ResourceNotFoundException.of("Season", seasonId));

        LocalDate asOf = season.getStartDate();
        List<PlayerSeason> history = playerSeasonRepository.findHistoryByPlayerIdBefore(playerId, asOf);
        Optional<Season> priorSeason = seasonRepository.findFirstByStartDateLessThanOrderByStartDateDesc(asOf);
        List<PlayerSeason> squad = priorSeason
                .map(prior -> playerSeasonRepository.findByClubIdAndSeasonId(targetClubId, prior.getId()))
                .orElseGet(List::of);
        LocalDate injurySince = asOf.minusYears(1);
        List<Injury> injuries = injuryRepository.findByPlayerIdAndStartDateGreaterThanEqual(playerId, injurySince);
        List<Contract> contracts = contractRepository.findByPlayerIdOrderByEndDateDesc(playerId);
        Optional<ClubSeason> clubSeason = priorSeason
                .flatMap(prior -> clubSeasonRepository.findByClubIdAndSeasonId(targetClubId, prior.getId()));
        Optional<PlayerSeason> mostRecent = history.stream().findFirst();

        return new PredictionContext(
                player,
                targetClub,
                season,
                history,
                squad,
                injuries,
                contracts,
                clubSeason,
                mostRecent
        );
    }
}
