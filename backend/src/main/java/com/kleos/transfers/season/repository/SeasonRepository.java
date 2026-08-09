package com.kleos.transfers.season.repository;

import com.kleos.transfers.season.entity.Season;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for season identity records.
 */
public interface SeasonRepository extends JpaRepository<Season, UUID> {

    /**
     * Loads active seasons matching one of the given normalized labels.
     * Used by bulk import to detect duplicates in a single query.
     */
    @Query("select s from Season s where s.labelNormalized in :normalizedLabels")
    List<Season> findAllByNormalizedLabel(@Param("normalizedLabels") Collection<String> normalizedLabels);

    Optional<Season> findFirstByStartDateLessThanOrderByStartDateDesc(LocalDate before);

    /**
     * Newest season that already has PlayerSeason rows — skips empty predict-to shells (e.g. 2026/27).
     */
    @Query("""
            select s from Season s
            where exists (
                select 1 from PlayerSeason ps
                where ps.season = s
            )
            order by s.startDate desc
            """)
    List<Season> findSeasonsWithPlayerDataOrderByStartDateDesc();
}
