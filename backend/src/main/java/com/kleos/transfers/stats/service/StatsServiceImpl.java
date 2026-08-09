package com.kleos.transfers.stats.service;

import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.domain.CareerMetric;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.stats.domain.LeagueCode;
import com.kleos.transfers.stats.dto.FitRouteResponse;
import com.kleos.transfers.stats.dto.LeaderboardEntryResponse;
import com.kleos.transfers.stats.dto.LeagueBoardsResponse;
import com.kleos.transfers.stats.entity.LeagueCareerTotal;
import com.kleos.transfers.stats.repository.LeagueCareerTotalRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private static final String LOADED_SEASONS_NOTE =
            "Career totals within loaded FBref seasons (since 2016/17) — not full historical all-time.";

    private final PlayerSeasonRepository playerSeasonRepository;
    private final SeasonRepository seasonRepository;
    private final LeagueCareerTotalRepository leagueCareerTotalRepository;
    private final FitRoutesService fitRoutesService;

    @Override
    public List<LeagueBoardsResponse> trending(UUID seasonId, int limit) {
        Season season = resolveSeason(seasonId);
        int capped = cap(limit);
        return Arrays.stream(LeagueCode.values())
                .map(league -> new LeagueBoardsResponse(
                        league,
                        league.tournamentName(),
                        season.getId(),
                        season.getLabel(),
                        null,
                        mapRows(playerSeasonRepository.findSeasonGoalLeaders(
                                league.tournamentName(), season.getId(), capped)),
                        mapRows(playerSeasonRepository.findSeasonAssistLeaders(
                                league.tournamentName(), season.getId(), capped))
                ))
                .toList();
    }

    @Override
    public List<LeagueBoardsResponse> allTime(int limit) {
        int capped = cap(limit);
        return Arrays.stream(LeagueCode.values())
                .map(league -> boardsForAllTime(league, capped))
                .toList();
    }

    @Override
    public List<FitRouteResponse> highestFitRoutes(int limit) {
        return fitRoutesService.highestFitRoutes(limit);
    }

    private LeagueBoardsResponse boardsForAllTime(LeagueCode league, int limit) {
        if (leagueCareerTotalRepository.existsByLeagueCode(league)) {
            List<LeagueCareerTotal> scorers = leagueCareerTotalRepository
                    .findByLeagueCodeAndMetricOrderByRankAsc(
                            league, CareerMetric.GOALS, PageRequest.of(0, limit));
            List<LeagueCareerTotal> assisters = leagueCareerTotalRepository
                    .findByLeagueCodeAndMetricOrderByRankAsc(
                            league, CareerMetric.ASSISTS, PageRequest.of(0, limit));
            String note = scorers.isEmpty()
                    ? LOADED_SEASONS_NOTE
                    : "Official/Wikipedia-curated career leaders (as of "
                            + scorers.getFirst().getAsOfDate()
                            + "). Linked when a matching Kleos player exists.";
            return new LeagueBoardsResponse(
                    league,
                    league.tournamentName(),
                    null,
                    null,
                    note,
                    mapCareer(scorers),
                    mapCareer(assisters)
            );
        }
        return new LeagueBoardsResponse(
                league,
                league.tournamentName(),
                null,
                null,
                LOADED_SEASONS_NOTE,
                mapRows(playerSeasonRepository.findAllTimeGoalLeaders(league.tournamentName(), limit)),
                mapRows(playerSeasonRepository.findAllTimeAssistLeaders(league.tournamentName(), limit))
        );
    }

    private Season resolveSeason(UUID seasonId) {
        if (seasonId != null) {
            return seasonRepository.findById(seasonId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Season", seasonId));
        }
        // Prefer latest season with boards data — not an empty upcoming predict-to shell.
        return seasonRepository.findSeasonsWithPlayerDataOrderByStartDateDesc().stream()
                .findFirst()
                .or(() -> seasonRepository.findAll(PageRequest.of(0, 1, org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "startDate")))
                        .stream()
                        .findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("No seasons available"));
    }

    private static int cap(int limit) {
        if (limit < 1) {
            return 3;
        }
        return Math.min(limit, 25);
    }

    private static List<LeaderboardEntryResponse> mapRows(List<Object[]> rows) {
        return rows.stream().map(StatsServiceImpl::mapRow).toList();
    }

    private static LeaderboardEntryResponse mapRow(Object[] row) {
        return new LeaderboardEntryResponse(
                (UUID) row[0],
                (String) row[1],
                (UUID) row[2],
                (String) row[3],
                ((Number) row[4]).longValue(),
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue(),
                ((Number) row[7]).longValue(),
                ((Number) row[8]).longValue()
        );
    }

    private static List<LeaderboardEntryResponse> mapCareer(List<LeagueCareerTotal> rows) {
        return rows.stream().map(row -> {
            UUID playerId = row.getPlayer() == null ? null : row.getPlayer().getId();
            long goals = row.getMetric() == CareerMetric.GOALS ? row.getTotal() : 0L;
            long assists = row.getMetric() == CareerMetric.ASSISTS ? row.getTotal() : 0L;
            return new LeaderboardEntryResponse(
                    playerId,
                    row.getPlayerName(),
                    null,
                    null,
                    goals,
                    assists,
                    0L,
                    0L,
                    0L
            );
        }).toList();
    }
}
