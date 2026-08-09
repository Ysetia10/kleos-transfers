package com.kleos.transfers.player.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Latest club attachment for a player, derived from PlayerSeason history.
 */
public interface LatestClubView {

    UUID getPlayerId();

    UUID getClubId();

    String getClubName();

    String getSeasonLabel();

    LocalDate getSeasonStartDate();
}
