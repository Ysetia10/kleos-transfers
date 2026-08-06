package com.kleos.transfers.club.repository;

import com.kleos.transfers.club.entity.Club;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for club identity records.
 */
public interface ClubRepository extends JpaRepository<Club, UUID> {

    /**
     * Loads active clubs matching one of the given normalized names.
     * Used by bulk import to detect duplicates in a single query.
     */
    @Query("select c from Club c where c.nameNormalized in :normalizedNames")
    List<Club> findAllByNormalizedName(@Param("normalizedNames") Collection<String> normalizedNames);

    Optional<Club> findByFbrefId(String fbrefId);

    List<Club> findAllByFbrefIdIn(Collection<String> fbrefIds);

    boolean existsByNameNormalizedAndCountryCode(String nameNormalized, String countryCode);

    boolean existsByNameNormalizedAndCountryCodeAndIdNot(
            String nameNormalized,
            String countryCode,
            UUID id
    );

    boolean existsByFbrefId(String fbrefId);

    boolean existsByFbrefIdAndIdNot(String fbrefId, UUID id);
}
