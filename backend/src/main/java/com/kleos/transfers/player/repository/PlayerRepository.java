package com.kleos.transfers.player.repository;

import com.kleos.transfers.player.entity.Player;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Case- and accent-insensitive match on {@code full_name} and/or nationality codes.
     * Native SQL so {@code unaccent} works; soft-delete filter is explicit.
     *
     * <p>{@code codes} must be non-empty when {@code hasCodes} is true. When false, pass a
     * dummy singleton so {@code IN (:codes)} stays syntactically valid.
     */
    @Query(
            value = """
                    SELECT * FROM players p
                    WHERE p.deleted_at IS NULL
                      AND (
                        unaccent(lower(p.full_name)) LIKE unaccent(lower(concat('%', :q, '%'))) ESCAPE '\\'
                        OR (CAST(:hasCodes AS boolean) AND p.nationality IN (:codes))
                      )
                    ORDER BY p.full_name
                    """,
            countQuery = """
                    SELECT count(*) FROM players p
                    WHERE p.deleted_at IS NULL
                      AND (
                        unaccent(lower(p.full_name)) LIKE unaccent(lower(concat('%', :q, '%'))) ESCAPE '\\'
                        OR (CAST(:hasCodes AS boolean) AND p.nationality IN (:codes))
                      )
                    """,
            nativeQuery = true
    )
    Page<Player> search(
            @Param("q") String q,
            @Param("hasCodes") boolean hasCodes,
            @Param("codes") Collection<String> codes,
            Pageable pageable
    );
}
