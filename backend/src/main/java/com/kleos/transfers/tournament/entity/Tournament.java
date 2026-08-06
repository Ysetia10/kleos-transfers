package com.kleos.transfers.tournament.entity;

import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.domain.Confederation;
import com.kleos.transfers.domain.TournamentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Persistent identity record for a football competition.
 *
 * <p>Season editions, tables, and fixtures belong to historical entities, not here.
 */
@Entity
@Table(name = "tournaments")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tournament extends IdentityEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 160)
    private String nameNormalized;

    @Column(name = "short_name", nullable = false, length = 40)
    private String shortName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Confederation confederation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TournamentType type;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    public Tournament(
            String name,
            String shortName,
            Confederation confederation,
            TournamentType type,
            String countryCode
    ) {
        update(name, shortName, confederation, type, countryCode);
    }

    public void update(
            String name,
            String shortName,
            Confederation confederation,
            TournamentType type,
            String countryCode
    ) {
        this.name = name == null ? null : name.trim();
        this.nameNormalized = this.name == null ? null : this.name.toLowerCase(Locale.ROOT);
        this.shortName = shortName == null ? null : shortName.trim();
        this.confederation = confederation;
        this.type = type;
        if (countryCode == null || countryCode.isBlank()) {
            this.countryCode = null;
        } else {
            this.countryCode = countryCode.trim().toUpperCase(Locale.ROOT);
        }
    }

    @Override
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        super.softDelete();
        // Keep historical FK targets while freeing the active uniqueness slot.
        this.nameNormalized = this.nameNormalized + "#" + getId();
    }
}
