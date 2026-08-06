package com.kleos.transfers.managerseason.repository;

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
}
