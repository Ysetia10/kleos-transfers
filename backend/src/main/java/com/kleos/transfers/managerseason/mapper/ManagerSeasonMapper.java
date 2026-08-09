package com.kleos.transfers.managerseason.mapper;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.domain.TacticalSystem;
import com.kleos.transfers.domain.TempoProfile;
import com.kleos.transfers.manager.entity.Manager;
import com.kleos.transfers.managerseason.dto.ManagerSeasonResponse;
import com.kleos.transfers.managerseason.entity.ManagerSeason;
import com.kleos.transfers.season.entity.Season;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Maps manager-season persistence models to and from API contracts.
 */
@Component
public class ManagerSeasonMapper {

    public ManagerSeason toEntity(
            Manager manager,
            Club club,
            Season season,
            TacticalSystem tacticalSystem,
            TempoProfile tempo,
            BigDecimal youthMinutesPct
    ) {
        return new ManagerSeason(manager, club, season, tacticalSystem, tempo, youthMinutesPct);
    }

    public ManagerSeasonResponse toResponse(ManagerSeason managerSeason) {
        Manager manager = managerSeason.getManager();
        Club club = managerSeason.getClub();
        Season season = managerSeason.getSeason();
        return new ManagerSeasonResponse(
                managerSeason.getId(),
                manager.getId(),
                manager.getFullName(),
                club.getId(),
                club.getName(),
                season.getId(),
                season.getLabel(),
                managerSeason.getTacticalSystem(),
                managerSeason.getTempo(),
                managerSeason.getYouthMinutesPct(),
                managerSeason.getCreatedAt(),
                managerSeason.getUpdatedAt()
        );
    }
}
