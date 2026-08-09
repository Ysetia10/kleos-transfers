package com.kleos.transfers.stats.dto;

import com.kleos.transfers.stats.domain.LeagueCode;
import java.util.List;
import java.util.UUID;

/**
 * Top scorers and assisters for one league (season or all-time).
 */
public record LeagueBoardsResponse(
        LeagueCode league,
        String tournamentName,
        UUID seasonId,
        String seasonLabel,
        String coverageNote,
        List<LeaderboardEntryResponse> topScorers,
        List<LeaderboardEntryResponse> topAssisters
) {
}
