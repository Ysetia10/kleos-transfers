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
 *
 * <p>Active uniqueness is {@code (nameNormalized, countryCode)}. When present,
 * {@code fbrefId} is also unique and is the preferred ingest match key.
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

    @Column(name = "fbref_id", length = 120)
    private String fbrefId;

    @Column(name = "crest_url", length = 1000)
    private String crestUrl;

    @Column(name = "crest_attribution", length = 500)
    private String crestAttribution;

    @Column(name = "crest_license", length = 80)
    private String crestLicense;

    @Column(name = "crest_source", length = 40)
    private String crestSource;

    public Club(String name, String shortName, String countryCode, Integer foundedYear) {
        this(name, shortName, countryCode, foundedYear, null);
    }

    public Club(String name, String shortName, String countryCode, Integer foundedYear, String fbrefId) {
        update(name, shortName, countryCode, foundedYear, fbrefId);
    }

    public void update(String name, String shortName, String countryCode, Integer foundedYear, String fbrefId) {
        this.name = name == null ? null : name.trim();
        this.nameNormalized = this.name == null ? null : this.name.toLowerCase(Locale.ROOT);
        this.shortName = shortName == null ? null : shortName.trim();
        this.countryCode = countryCode == null ? null : countryCode.trim().toUpperCase(Locale.ROOT);
        this.foundedYear = foundedYear;
        this.fbrefId = blankToNull(fbrefId);
    }

    public void updateMedia(String crestUrl, String crestAttribution, String crestLicense, String crestSource) {
        this.crestUrl = blankToNull(crestUrl);
        this.crestAttribution = blankToNull(crestAttribution);
        this.crestLicense = blankToNull(crestLicense);
        this.crestSource = blankToNull(crestSource);
    }

    @Override
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        super.softDelete();
        this.nameNormalized = this.nameNormalized + "#" + getId();
        if (this.fbrefId != null) {
            this.fbrefId = this.fbrefId + "#" + getId();
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
