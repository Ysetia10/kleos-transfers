package com.kleos.transfers.stats.service;

import com.kleos.transfers.common.exception.ResourceNotFoundException;
import com.kleos.transfers.playerseason.repository.PlayerSeasonRepository;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.season.repository.SeasonRepository;
import com.kleos.transfers.stats.domain.LeagueCode;
import com.kleos.transfers.stats.dto.LeaderboardEntryResponse;
import com.kleos.transfers.stats.dto.LeagueBoardsResponse;
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

    private final PlayerSeasonRepository playerSeasonRepository;
    private final SeasonRepository seasonRepository;

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
                .map(league -> new LeagueBoardsResponse(
                        league,
                        league.tournamentName(),
                        null,
                        null,
                        mapRows(playerSeasonRepository.findAllTimeGoalLeaders(
                                league.tournamentName(), capped)),
                        mapRows(playerSeasonRepository.findAllTimeAssistLeaders(
                                league.tournamentName(), capped))
                ))
                .toList();
    }

    private Season resolveSeason(UUID seasonId) {
        if (seasonId != null) {
            return seasonRepository.findById(seasonId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Season", seasonId));
        }
        return seasonRepository.findAll(PageRequest.of(0, 1, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "startDate")))
                .stream()
                .findFirst()
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
}
