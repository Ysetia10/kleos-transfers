package com.kleos.transfers.player.entity;

import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Persistent identity record for a football player.
 *
 * <p>Active uniqueness is {@code (fullNameNormalized, dateOfBirth, nationality)}.
 * When present, {@code fbrefId} is also unique and is the preferred ingest match key.
 */
@Entity
@Table(name = "players")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player extends IdentityEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "full_name_normalized", nullable = false, length = 160)
    private String fullNameNormalized;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 3)
    private String nationality;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_foot", length = 5)
    private PreferredFoot preferredFoot;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_position", nullable = false, length = 3)
    private Position primaryPosition;

    @Column(name = "fbref_id", length = 120)
    private String fbrefId;

    public Player(
            String fullName,
            LocalDate dateOfBirth,
            String nationality,
            Integer heightCm,
            PreferredFoot preferredFoot,
            Position primaryPosition
    ) {
        this(fullName, dateOfBirth, nationality, heightCm, preferredFoot, primaryPosition, null);
    }

    public Player(
            String fullName,
            LocalDate dateOfBirth,
            String nationality,
            Integer heightCm,
            PreferredFoot preferredFoot,
            Position primaryPosition,
            String fbrefId
    ) {
        update(fullName, dateOfBirth, nationality, heightCm, preferredFoot, primaryPosition, fbrefId);
    }

    public void update(
            String fullName,
            LocalDate dateOfBirth,
            String nationality,
            Integer heightCm,
            PreferredFoot preferredFoot,
            Position primaryPosition,
            String fbrefId
    ) {
        this.fullName = fullName == null ? null : fullName.trim();
        this.fullNameNormalized = this.fullName == null ? null : this.fullName.toLowerCase(Locale.ROOT);
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality == null ? null : nationality.trim().toUpperCase(Locale.ROOT);
        this.heightCm = heightCm;
        this.preferredFoot = preferredFoot;
        this.primaryPosition = primaryPosition;
        this.fbrefId = normalizeFbrefId(fbrefId);
    }

    @Override
    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        super.softDelete();
        this.fullNameNormalized = this.fullNameNormalized + "#" + getId();
        if (this.fbrefId != null) {
            this.fbrefId = this.fbrefId + "#" + getId();
        }
    }

    private static String normalizeFbrefId(String fbrefId) {
        if (fbrefId == null || fbrefId.isBlank()) {
            return null;
        }
        return fbrefId.trim();
    }
}
