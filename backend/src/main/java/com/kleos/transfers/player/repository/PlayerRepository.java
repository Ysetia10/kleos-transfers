package com.kleos.transfers.player.repository;

import com.kleos.transfers.player.entity.Player;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for player identity records.
 */
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    /**
     * Loads active players whose lowercase name matches one of the given names.
     * Used by bulk import to detect duplicates in a single query.
     */
    @Query("select p from Player p where lower(p.fullName) in :normalizedNames")
    List<Player> findAllByNormalizedName(@Param("normalizedNames") Collection<String> normalizedNames);

    Optional<Player> findByFbrefId(String fbrefId);

    List<Player> findAllByFbrefIdIn(Collection<String> fbrefIds);

    boolean existsByFullNameNormalizedAndDateOfBirthAndNationality(
            String fullNameNormalized,
            LocalDate dateOfBirth,
            String nationality
    );

    boolean existsByFullNameNormalizedAndDateOfBirthAndNationalityAndIdNot(
            String fullNameNormalized,
            LocalDate dateOfBirth,
            String nationality,
            UUID id
    );

    boolean existsByFbrefId(String fbrefId);

    boolean existsByFbrefIdAndIdNot(String fbrefId, UUID id);
}
