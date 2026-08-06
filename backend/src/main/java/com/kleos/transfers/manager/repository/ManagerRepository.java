package com.kleos.transfers.manager.repository;

import com.kleos.transfers.manager.entity.Manager;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for manager identity records.
 */
public interface ManagerRepository extends JpaRepository<Manager, UUID> {

    /**
     * Loads active managers whose lowercase name matches one of the given names.
     * Used by bulk import to detect duplicates in a single query.
     */
    @Query("select m from Manager m where lower(m.fullName) in :normalizedNames")
    List<Manager> findAllByNormalizedName(@Param("normalizedNames") Collection<String> normalizedNames);
}
