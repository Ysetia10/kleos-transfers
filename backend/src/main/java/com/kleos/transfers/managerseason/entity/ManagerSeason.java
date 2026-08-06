package com.kleos.transfers.managerseason.entity;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.manager.entity.Manager;
import com.kleos.transfers.season.entity.Season;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Historical record of a manager appointed at a club for a season.
 *
 * <p>Tactical philosophy and style belong here later only when a prediction
 * feature needs them. Mid-season changes are modeled as separate rows for
 * different managers at the same club and season.
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

    @Column(name = "uniqueness_key", nullable = false, length = 160)
    private String uniquenessKey;

    public ManagerSeason(Manager manager, Club club, Season season) {
        this.manager = manager;
        this.club = club;
        this.season = season;
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
