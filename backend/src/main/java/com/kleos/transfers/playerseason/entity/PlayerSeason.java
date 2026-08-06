package com.kleos.transfers.playerseason.entity;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.player.entity.Player;
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
 * Historical record of a player's performance for one club in one season.
 *
 * <p>Mid-season transfers produce a second row for the destination club.
 * Permanent identity fields (nationality, date of birth, …) stay on {@link Player}.
 */
@Entity
@Table(name = "player_seasons")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerSeason extends IdentityEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false)
    private Integer appearances;

    @Column(name = "minutes_played", nullable = false)
    private Integer minutesPlayed;

    @Column(nullable = false)
    private Integer goals;

    @Column(nullable = false)
    private Integer assists;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal xg;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal xa;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_position", nullable = false, length = 3)
    private Position primaryPosition;

    @Column(name = "uniqueness_key", nullable = false, length = 160)
    private String uniquenessKey;

    public PlayerSeason(
            Player player,
            Club club,
            Season season,
            Integer appearances,
            Integer minutesPlayed,
            Integer goals,
            Integer assists,
            BigDecimal xg,
            BigDecimal xa,
            Position primaryPosition
    ) {
        this.player = player;
        this.club = club;
        this.season = season;
        updateStats(appearances, minutesPlayed, goals, assists, xg, xa, primaryPosition);
        refreshUniquenessKey();
    }

    public void reassign(
            Player player,
            Club club,
            Season season,
            Integer appearances,
            Integer minutesPlayed,
            Integer goals,
            Integer assists,
            BigDecimal xg,
            BigDecimal xa,
            Position primaryPosition
    ) {
        this.player = player;
        this.club = club;
        this.season = season;
        updateStats(appearances, minutesPlayed, goals, assists, xg, xa, primaryPosition);
        refreshUniquenessKey();
    }

    public void updateStats(
            Integer appearances,
            Integer minutesPlayed,
            Integer goals,
            Integer assists,
            BigDecimal xg,
            BigDecimal xa,
            Position primaryPosition
    ) {
        this.appearances = appearances;
        this.minutesPlayed = minutesPlayed;
        this.goals = goals;
        this.assists = assists;
        this.xg = xg;
        this.xa = xa;
        this.primaryPosition = primaryPosition;
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
        this.uniquenessKey = player.getId() + ":" + club.getId() + ":" + season.getId();
    }
}
