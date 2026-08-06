package com.kleos.transfers.club.entity;

import com.kleos.transfers.common.entity.IdentityEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Persistent identity record for a football club.
 */
@Entity
@Table(name = "clubs")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club extends IdentityEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 160)
    private String nameNormalized;

    @Column(name = "short_name", nullable = false, length = 40)
    private String shortName;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Column(name = "founded_year")
    private Integer foundedYear;

    public Club(String name, String shortName, String countryCode, Integer foundedYear) {
        update(name, shortName, countryCode, foundedYear);
    }

    public void update(String name, String shortName, String countryCode, Integer foundedYear) {
        this.name = name == null ? null : name.trim();
        this.nameNormalized = this.name == null ? null : this.name.toLowerCase(Locale.ROOT);
        this.shortName = shortName == null ? null : shortName.trim();
        this.countryCode = countryCode == null ? null : countryCode.trim().toUpperCase(Locale.ROOT);
        this.foundedYear = foundedYear;
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
