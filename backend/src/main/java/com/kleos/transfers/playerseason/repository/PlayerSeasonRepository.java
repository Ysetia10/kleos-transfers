package com.kleos.transfers.playerseason.repository;

import com.kleos.transfers.playerseason.entity.PlayerSeason;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for player-season performance records.
 */
public interface PlayerSeasonRepository extends JpaRepository<PlayerSeason, UUID> {

    /**
     * Loads active player-seasons whose uniqueness keys match any of the given keys.
     */
    @Query("select ps from PlayerSeason ps where ps.uniquenessKey in :keys")
    List<PlayerSeason> findAllByUniquenessKeyIn(@Param("keys") Collection<String> keys);
}
