package com.kleos.transfers.transfer.entity;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.domain.TransferStatus;
import com.kleos.transfers.domain.TransferType;
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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Historical or market-signal record of a player moving between clubs (or free agency).
 *
 * <p>Does not generate predictions on create; prediction scenarios may reference these rows later.
 */
@Entity
@Table(name = "transfers")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer extends IdentityEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_club_id")
    private Club fromClub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_club_id")
    private Club toClub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(name = "fee_eur", precision = 14, scale = 2)
    private BigDecimal feeEur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransferStatus status;

    @Column(length = 64)
    private String source;

    @Column(length = 500)
    private String notes;

    @Column(name = "uniqueness_key", nullable = false, length = 240)
    private String uniquenessKey;

    public Transfer(
            Player player,
            Club fromClub,
            Club toClub,
            Season season,
            LocalDate transferDate,
            BigDecimal feeEur,
            TransferType type,
            TransferStatus status,
            String source,
            String notes
    ) {
        apply(player, fromClub, toClub, season, transferDate, feeEur, type, status, source, notes);
    }

    public void reassign(
            Player player,
            Club fromClub,
            Club toClub,
            Season season,
            LocalDate transferDate,
            BigDecimal feeEur,
            TransferType type,
            TransferStatus status,
            String source,
            String notes
    ) {
        apply(player, fromClub, toClub, season, transferDate, feeEur, type, status, source, notes);
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
            Club fromClub,
            Club toClub,
            Season season,
            LocalDate transferDate,
            BigDecimal feeEur,
            TransferType type,
            TransferStatus status,
            String source,
            String notes
    ) {
        this.player = player;
        this.fromClub = fromClub;
        this.toClub = toClub;
        this.season = season;
        this.transferDate = transferDate;
        this.feeEur = feeEur;
        this.type = type;
        this.status = status == null ? TransferStatus.COMPLETED : status;
        this.source = blankToNull(source);
        this.notes = blankToNull(notes);
        refreshUniquenessKey();
    }

    private void refreshUniquenessKey() {
        this.uniquenessKey = player.getId()
                + ":"
                + transferDate
                + ":"
                + clubKey(fromClub)
                + ":"
                + clubKey(toClub)
                + ":"
                + type.name()
                + ":"
                + status.name();
    }

    private static String clubKey(Club club) {
        return club == null ? "none" : club.getId().toString();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
