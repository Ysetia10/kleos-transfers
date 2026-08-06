package com.kleos.transfers.injury.entity;

import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.domain.InjurySeverity;
import com.kleos.transfers.player.entity.Player;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Historical record of a single injury spell for a player.
 *
 * <p>Used as availability and adaptation context: recent time out is a strong
 * signal for minutes predictions after a transfer.
 */
@Entity
@Table(name = "injuries")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Injury extends IdentityEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "injury_type", nullable = false, length = 80)
    private String injuryType;

    @Column(name = "injury_type_normalized", nullable = false, length = 80)
    private String injuryTypeNormalized;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InjurySeverity severity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "uniqueness_key", nullable = false, length = 200)
    private String uniquenessKey;

    public Injury(
            Player player,
            String injuryType,
            InjurySeverity severity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        apply(player, injuryType, severity, startDate, endDate);
    }

    public void reassign(
            Player player,
            String injuryType,
            InjurySeverity severity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        apply(player, injuryType, severity, startDate, endDate);
    }

    /**
     * Days unavailable, inclusive of both end points. Null while the spell is ongoing.
     */
    public Integer getDaysOut() {
        if (endDate == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public boolean isOngoing() {
        return endDate == null;
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
            String injuryType,
            InjurySeverity severity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.player = player;
        this.injuryType = injuryType.trim();
        this.injuryTypeNormalized = this.injuryType.toLowerCase(Locale.ROOT);
        this.severity = severity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.uniquenessKey = player.getId() + ":" + startDate + ":" + this.injuryTypeNormalized;
    }
}
