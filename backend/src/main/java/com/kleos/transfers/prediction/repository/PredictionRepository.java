package com.kleos.transfers.prediction.repository;

import com.kleos.transfers.prediction.entity.Prediction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for predictions.
 */
public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    @Query("""
            select distinct p from Prediction p
            join fetch p.run
            join fetch p.player
            join fetch p.targetClub
            join fetch p.season
            left join fetch p.explanations
            left join fetch p.evaluation
            where p.id = :id
            """)
    Optional<Prediction> findDetailedById(@Param("id") UUID id);

    @Query("""
            select distinct p from Prediction p
            join fetch p.run
            join fetch p.player
            join fetch p.targetClub
            join fetch p.season
            left join fetch p.explanations
            left join fetch p.evaluation
            where p.run.id = :runId
            """)
    List<Prediction> findDetailedByRunId(@Param("runId") UUID runId);

    Page<Prediction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select p from Prediction p
            join fetch p.player
            join fetch p.targetClub
            join fetch p.season
            order by p.compatibilityScore desc, p.createdAt desc
            """)
    List<Prediction> findTopByCompatibility(Pageable pageable);
}
