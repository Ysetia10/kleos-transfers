package com.kleos.transfers.club.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Current manager appointment for a club, derived from ManagerSeason history.
 */
public interface CurrentManagerView {

    UUID getClubId();

    UUID getManagerId();

    String getManagerName();

    String getSeasonLabel();

    UUID getSeasonId();

    String getTacticalSystem();

    String getTempo();

    BigDecimal getYouthMinutesPct();

    /** True when this manager has no earlier appointment at the same club. */
    Boolean getFirstSeasonAtClub();
}
