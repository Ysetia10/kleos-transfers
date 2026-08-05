package com.kleos.transfers.entity;

import com.kleos.transfers.domain.Position;
import com.kleos.transfers.domain.PreferredFoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Persistent identity record for a football player.
 */
@Entity
@Table(name = "players")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 3)
    private String nationality;

    @Column(name = "height_cm", nullable = false)
    private Integer heightCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_foot", nullable = false, length = 5)
    private PreferredFoot preferredFoot;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_position", nullable = false, length = 3)
    private Position primaryPosition;

    public Player(
            String fullName,
            LocalDate dateOfBirth,
            String nationality,
            Integer heightCm,
            PreferredFoot preferredFoot,
            Position primaryPosition
    ) {
        update(fullName, dateOfBirth, nationality, heightCm, preferredFoot, primaryPosition);
    }

    public void update(
            String fullName,
            LocalDate dateOfBirth,
            String nationality,
            Integer heightCm,
            PreferredFoot preferredFoot,
            Position primaryPosition
    ) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality == null ? null : nationality.trim().toUpperCase(Locale.ROOT);
        this.heightCm = heightCm;
        this.preferredFoot = preferredFoot;
        this.primaryPosition = primaryPosition;
    }
}
