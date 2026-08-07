package com.kleos.transfers.stats.service;

import com.kleos.transfers.stats.domain.LeagueCode;
import com.kleos.transfers.stats.dto.LeagueBoardsResponse;
import java.util.List;
import java.util.UUID;

public interface StatsService {

    /**
     * Top scorers/assisters for each league in the given season (defaults to latest season).
     */
    List<LeagueBoardsResponse> trending(UUID seasonId, int limit);

    /**
     * Career totals within each league across all ingested seasons.
     */
    List<LeagueBoardsResponse> allTime(int limit);
}
