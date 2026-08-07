package com.kleos.transfers.managerseason.repository;

import com.kleos.transfers.club.dto.CurrentManagerView;
import com.kleos.transfers.managerseason.entity.ManagerSeason;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for manager-season appointment records.
 */
public interface ManagerSeasonRepository extends JpaRepository<ManagerSeason, UUID> {

    /**
     * Loads active manager-seasons whose uniqueness keys match any of the given keys.
     */
    @Query("select ms from ManagerSeason ms where ms.uniquenessKey in :keys")
    List<ManagerSeason> findAllByUniquenessKeyIn(@Param("keys") Collection<String> keys);

    /**
     * Latest manager per club (most recent season start, then most recently recorded appointment).
     */
    @Query(
            value = """
                    SELECT DISTINCT ON (ms.club_id)
                           ms.club_id AS "clubId",
                           m.id AS "managerId",
                           m.full_name AS "managerName",
                           s.label AS "seasonLabel"
                    FROM manager_seasons ms
                    JOIN managers m ON m.id = ms.manager_id
                    JOIN seasons s ON s.id = ms.season_id
                    WHERE ms.deleted_at IS NULL
                      AND m.deleted_at IS NULL
                      AND ms.club_id IN (:clubIds)
                    ORDER BY ms.club_id, s.start_date DESC, ms.created_at DESC
                    """,
            nativeQuery = true
    )
    List<CurrentManagerView> findCurrentManagersByClubIds(@Param("clubIds") Collection<UUID> clubIds);
}
