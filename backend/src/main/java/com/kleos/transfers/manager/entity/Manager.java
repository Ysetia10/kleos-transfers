package com.kleos.transfers.manager.entity;

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
 * Persistent identity record for a football manager.
 *
 * <p>Club appointments, tactical style, and results belong to {@code ManagerSeason}.
 */
@Entity
@Table(name = "managers")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Manager extends IdentityEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 3)
    private String nationality;

    public Manager(String fullName, LocalDate dateOfBirth, String nationality) {
        update(fullName, dateOfBirth, nationality);
    }

    public void update(String fullName, LocalDate dateOfBirth, String nationality) {
        this.fullName = fullName == null ? null : fullName.trim();
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality == null ? null : nationality.trim().toUpperCase(Locale.ROOT);
    }
}
