package com.kleos.transfers.contract.entity;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.player.entity.Player;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Historical record of a player's contract window at a club.
 *
 * <p>Used as prediction context for expiry pressure and renewal likelihood.
 * A renewal is a new Contract row rather than an edit of the previous one.
 */
@Entity
@Table(name = "contracts")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contract extends IdentityEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "release_clause_eur", precision = 14, scale = 2)
    private BigDecimal releaseClauseEur;

    @Column(name = "uniqueness_key", nullable = false, length = 200)
    private String uniquenessKey;

    public Contract(
            Player player,
            Club club,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal releaseClauseEur
    ) {
        apply(player, club, startDate, endDate, releaseClauseEur);
    }

    public void reassign(
            Player player,
            Club club,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal releaseClauseEur
    ) {
        apply(player, club, startDate, endDate, releaseClauseEur);
    }

    @Override
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        super.softDelete();
        this.uniquenessKey = this.uniquenessKey + "#" + getId();
    }

    private void apply(
            Player player,
            Club club,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal releaseClauseEur
    ) {
        this.player = player;
        this.club = club;
        this.startDate = startDate;
        this.endDate = endDate;
        this.releaseClauseEur = releaseClauseEur;
        this.uniquenessKey = player.getId() + ":" + club.getId() + ":" + startDate;
    }
}
