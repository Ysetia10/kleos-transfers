package com.kleos.transfers.club.dto;

import java.util.UUID;

/**
 * Current manager appointment for a club, derived from ManagerSeason history.
 */
public interface CurrentManagerView {

    UUID getClubId();

    UUID getManagerId();

    String getManagerName();

    String getSeasonLabel();
}
