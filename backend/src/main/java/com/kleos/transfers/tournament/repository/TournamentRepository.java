package com.kleos.transfers.tournament.repository;

import com.kleos.transfers.tournament.entity.Tournament;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for tournament identity records.
 */
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    /**
     * Loads active tournaments matching one of the given normalized names.
     * Used by bulk import to detect duplicates in a single query.
     */
    @Query("select t from Tournament t where t.nameNormalized in :normalizedNames")
    List<Tournament> findAllByNormalizedName(@Param("normalizedNames") Collection<String> normalizedNames);
}
