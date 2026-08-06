package com.kleos.transfers.season.entity;

import com.kleos.transfers.common.entity.IdentityEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Persistent identity record for a football competition season.
 *
 * <p>A season is a named date range used as a foreign key by historical entities.
 * Fixtures, standings, and tournament membership do not belong here.
 */
@Entity
@Table(name = "seasons")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Season extends IdentityEntity {

    @Column(nullable = false, length = 20)
    private String label;

    @Column(name = "label_normalized", nullable = false, length = 60)
    private String labelNormalized;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    public Season(String label, LocalDate startDate, LocalDate endDate) {
        update(label, startDate, endDate);
    }

    public void update(String label, LocalDate startDate, LocalDate endDate) {
        this.label = label == null ? null : label.trim();
        this.labelNormalized = this.label == null ? null : this.label.toLowerCase(Locale.ROOT);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        super.softDelete();
        // Keep historical FK targets while freeing the active uniqueness slot.
        this.labelNormalized = this.labelNormalized + "#" + getId();
    }
}
