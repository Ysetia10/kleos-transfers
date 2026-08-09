package com.kleos.transfers.managerseason.entity;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.domain.TacticalSystem;
import com.kleos.transfers.domain.TempoProfile;
import com.kleos.transfers.manager.entity.Manager;
import com.kleos.transfers.season.entity.Season;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Historical record of a manager appointed at a club for a season.
 *
 * <p>Optional tactical attributes (system, tempo, youth minutes) feed absolute
 * club fit / recruitment signals. Mid-season changes are separate rows.
 */
@Entity
@Table(name = "manager_seasons")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ManagerSeason extends IdentityEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(name = "tactical_system", length = 20)
    private TacticalSystem tacticalSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tempo", length = 10)
    private TempoProfile tempo;

    @Column(name = "youth_minutes_pct", precision = 5, scale = 2)
    private BigDecimal youthMinutesPct;

    @Column(name = "uniqueness_key", nullable = false, length = 160)
    private String uniquenessKey;

    public ManagerSeason(Manager manager, Club club, Season season) {
        this(manager, club, season, null, null, null);
    }

    public ManagerSeason(
            Manager manager,
            Club club,
            Season season,
            TacticalSystem tacticalSystem,
            TempoProfile tempo,
            BigDecimal youthMinutesPct
    ) {
        this.manager = manager;
        this.club = club;
        this.season = season;
        updateTactics(tacticalSystem, tempo, youthMinutesPct);
        refreshUniquenessKey();
    }

    /**
     * Reassigns the appointment links and refreshes the uniqueness key.
     */
    public void reassign(Manager manager, Club club, Season season) {
        this.manager = manager;
        this.club = club;
        this.season = season;
        refreshUniquenessKey();
    }

    public void updateTactics(
            TacticalSystem tacticalSystem,
            TempoProfile tempo,
            BigDecimal youthMinutesPct
    ) {
        this.tacticalSystem = tacticalSystem;
        this.tempo = tempo;
        this.youthMinutesPct = youthMinutesPct;
    }

    @Override
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        super.softDelete();
        this.uniquenessKey = this.uniquenessKey + "#" + getId();
    }

    private void refreshUniquenessKey() {
        this.uniquenessKey = manager.getId() + ":" + club.getId() + ":" + season.getId();
    }
}
