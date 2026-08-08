package com.kleos.transfers.club.repository;

import com.kleos.transfers.club.entity.Club;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<Club> findAllByFbrefIdIn(Collection<String> fbrefIds);

    boolean existsByNameNormalizedAndCountryCode(String nameNormalized, String countryCode);

    boolean existsByNameNormalizedAndCountryCodeAndIdNot(
            String nameNormalized,
            String countryCode,
            UUID id
    );

    boolean existsByFbrefId(String fbrefId);

    boolean existsByFbrefIdAndIdNot(String fbrefId, UUID id);

    /**
     * Case- and accent-insensitive match on name/short name and/or country codes.
     *
     * <p>{@code codes} must be non-empty when {@code hasCodes} is true. When false, pass a
     * dummy singleton so {@code IN (:codes)} stays syntactically valid.
     */
    @Query(
            value = """
                    SELECT * FROM clubs c
                    WHERE c.deleted_at IS NULL
                      AND (
                        unaccent(lower(c.name)) LIKE unaccent(lower(concat('%', :q, '%'))) ESCAPE '\\'
                        OR unaccent(lower(c.short_name)) LIKE unaccent(lower(concat('%', :q, '%'))) ESCAPE '\\'
                        OR (CAST(:hasCodes AS boolean) AND c.country_code IN (:codes))
                      )
                    ORDER BY c.name
                    """,
            countQuery = """
                    SELECT count(*) FROM clubs c
                    WHERE c.deleted_at IS NULL
                      AND (
                        unaccent(lower(c.name)) LIKE unaccent(lower(concat('%', :q, '%'))) ESCAPE '\\'
                        OR unaccent(lower(c.short_name)) LIKE unaccent(lower(concat('%', :q, '%'))) ESCAPE '\\'
                        OR (CAST(:hasCodes AS boolean) AND c.country_code IN (:codes))
                      )
                    """,
            nativeQuery = true
    )
    Page<Club> search(
            @Param("q") String q,
            @Param("hasCodes") boolean hasCodes,
            @Param("codes") Collection<String> codes,
            Pageable pageable
    );
}
