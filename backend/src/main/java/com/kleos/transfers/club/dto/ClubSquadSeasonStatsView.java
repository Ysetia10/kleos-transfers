package com.kleos.transfers.club.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Squad coverage + U23 minutes share for a club-season pair.
 */
public interface ClubSquadSeasonStatsView {

    UUID getClubId();

    UUID getSeasonId();

    long getPlayerCount();

    BigDecimal getYouthMinutesPct();
}
