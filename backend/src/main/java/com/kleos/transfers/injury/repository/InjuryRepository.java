package com.kleos.transfers.injury.repository;

import com.kleos.transfers.injury.entity.Injury;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for injury records.
 */
public interface InjuryRepository extends JpaRepository<Injury, UUID> {

    /**
     * Loads active injuries whose uniqueness keys match any of the given keys.
     */
    @Query("select i from Injury i where i.uniquenessKey in :keys")
    List<Injury> findAllByUniquenessKeyIn(@Param("keys") Collection<String> keys);

    List<Injury> findByPlayerIdAndStartDateGreaterThanEqual(UUID playerId, LocalDate startDate);
}
