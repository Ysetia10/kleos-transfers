package com.kleos.transfers.clubseason.entity;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.season.entity.Season;
import com.kleos.transfers.tournament.entity.Tournament;
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
 * Historical record of a club competing in a season's primary tournament.
 *
 * <p>One active row per club per season. Cup/continental participation can be
 * modeled later without overloading this spine entity.
 */
@Entity
@Table(name = "club_seasons")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClubSeason extends IdentityEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "uniqueness_key", nullable = false, length = 120)
    private String uniquenessKey;

    public ClubSeason(Club club, Season season, Tournament tournament) {
        this.club = club;
        this.season = season;
        this.tournament = tournament;
        refreshUniquenessKey();
    }

    public void updateTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    /**
     * Reassigns the club/season pair and refreshes the uniqueness key.
     * Used when correcting a mis-linked historical row before soft delete.
     */
    public void reassign(Club club, Season season, Tournament tournament) {
        this.club = club;
        this.season = season;
        this.tournament = tournament;
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
        this.uniquenessKey = club.getId() + ":" + season.getId();
    }
}
