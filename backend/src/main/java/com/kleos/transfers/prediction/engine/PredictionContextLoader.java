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
import com.kleos.transfers.managerseason.repository.ManagerSeasonRepository;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.player.repository.PlayerRepository;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.domain.TransferStatus;
import com.kleos.transfers.transfer.entity.Transfer;
import com.kleos.transfers.transfer.repository.TransferRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final ManagerSeasonRepository managerSeasonRepository;
    private final TransferRepository transferRepository;

    /** Moves settled enough to reshape the depth chart the incoming player joins. */
    private static final List<TransferStatus> SQUAD_WINDOW_STATUSES = List.of(
            TransferStatus.COMPLETED,
            TransferStatus.ANNOUNCED
    );

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
        Optional<String> managerName = managerSeasonRepository
                .findCurrentManagersByClubIds(List.of(targetClubId))
                .stream()
                .findFirst()
                .map(view -> view.getManagerName());

        List<Transfer> window = transferRepository.findBySeasonIdAndClubIdAndStatusIn(
                seasonId,
                targetClubId,
                SQUAD_WINDOW_STATUSES
        );

        List<PlayerSeason> departing = departingSquadMembers(
                window, seasonId, targetClubId, playerId, squad, priorSeason, asOf
        );

        return new PredictionContext(
                player,
                targetClub,
                season,
                history,
                squad,
                injuries,
                contracts,
                clubSeason,
                mostRecent,
                managerName,
                departing,
                arrivingSquadMembers(window, targetClubId, playerId, asOf)
        );
    }

    /**
     * Prior-season rows of players leaving the target club in this window; their minutes are the
     * slot the club now has to fill.
     *
     * <p>Detects outs via {@code fromClub} on the club window <em>and</em> any season transfer for a
     * <p>Also covers free-agent / retirement exits ({@code toClub} null), and inferred contracts
     * that ended well before the prior season finished (mid-tenure departures without a clean
     * transfer {@code fromClub}).
     */
    private List<PlayerSeason> departingSquadMembers(
            List<Transfer> clubWindow,
            UUID seasonId,
            UUID targetClubId,
            UUID subjectId,
            List<PlayerSeason> squad,
            Optional<Season> priorSeason,
            LocalDate seasonStart
    ) {
        Set<UUID> leaving = new HashSet<>();
        for (Transfer transfer : clubWindow) {
            if (transfer.getFromClub() != null && targetClubId.equals(transfer.getFromClub().getId())) {
                leaving.add(transfer.getPlayer().getId());
            }
        }

        Set<UUID> squadIds = new HashSet<>();
        for (PlayerSeason row : squad) {
            squadIds.add(row.getPlayer().getId());
        }
        squadIds.remove(subjectId);
        if (!squadIds.isEmpty()) {
            for (Transfer transfer : transferRepository.findBySeasonIdAndPlayerIdInAndStatusIn(
                    seasonId, squadIds, SQUAD_WINDOW_STATUSES
            )) {
                if (isDepartureFromClub(transfer, targetClubId)) {
                    leaving.add(transfer.getPlayer().getId());
                } else if (isFreeExitFromClub(transfer, targetClubId)) {
                    leaving.add(transfer.getPlayer().getId());
                }
            }

            LocalDate priorEnd = priorSeason.map(Season::getEndDate).orElse(seasonStart);
            LocalDate earlyExitCutoff = priorEnd.minusDays(45);
            for (Contract contract : contractRepository.findByClubIdAndPlayerIdIn(targetClubId, squadIds)) {
                if (contract.getEndDate().isBefore(earlyExitCutoff)) {
                    leaving.add(contract.getPlayer().getId());
                }
            }
        }

        if (leaving.isEmpty()) {
            return List.of();
        }
        return squad.stream()
                .filter(row -> leaving.contains(row.getPlayer().getId()))
                .filter(row -> !row.getPlayer().getId().equals(subjectId))
                .toList();
    }

    private static boolean isDepartureFromClub(Transfer transfer, UUID targetClubId) {
        if (transfer.getFromClub() != null && targetClubId.equals(transfer.getFromClub().getId())) {
            return true;
        }
        // Prior-squad player arriving elsewhere this window — treat as leaving even if fromClub is null.
        return transfer.getToClub() != null && !targetClubId.equals(transfer.getToClub().getId());
    }

    /** Retirement / free release with no destination club recorded. */
    private static boolean isFreeExitFromClub(Transfer transfer, UUID targetClubId) {
        if (transfer.getToClub() != null) {
            return false;
        }
        return transfer.getFromClub() == null || targetClubId.equals(transfer.getFromClub().getId());
    }

    /**
     * Latest previous-club row for each other player arriving in this window, so incoming rivals
     * carry the level they were playing at before the move.
     */
    private List<PlayerSeason> arrivingSquadMembers(
            List<Transfer> window,
            UUID targetClubId,
            UUID subjectId,
            LocalDate asOf
    ) {
        Set<UUID> arriving = new LinkedHashSet<>();
        for (Transfer transfer : window) {
            if (transfer.getToClub() == null || !targetClubId.equals(transfer.getToClub().getId())) {
                continue;
            }
            UUID arrivalId = transfer.getPlayer().getId();
            if (!arrivalId.equals(subjectId)) {
                arriving.add(arrivalId);
            }
        }
        if (arriving.isEmpty()) {
            return List.of();
        }

        Map<UUID, PlayerSeason> latestByPlayer = new LinkedHashMap<>();
        for (PlayerSeason row : playerSeasonRepository.findHistoryByPlayerIdsBefore(arriving, asOf)) {
            latestByPlayer.putIfAbsent(row.getPlayer().getId(), row);
        }
        return List.copyOf(latestByPlayer.values());
    }
}
