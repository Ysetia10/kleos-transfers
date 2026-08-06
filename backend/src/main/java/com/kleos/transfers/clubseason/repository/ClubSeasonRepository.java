package com.kleos.transfers.clubseason.repository;

import com.kleos.transfers.clubseason.entity.ClubSeason;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for club-season historical records.
 */
public interface ClubSeasonRepository extends JpaRepository<ClubSeason, UUID> {

    /**
     * Loads active club-seasons whose uniqueness keys match any of the given keys.
     */
    @Query("select cs from ClubSeason cs where cs.uniquenessKey in :keys")
    List<ClubSeason> findAllByUniquenessKeyIn(@Param("keys") Collection<String> keys);
}
