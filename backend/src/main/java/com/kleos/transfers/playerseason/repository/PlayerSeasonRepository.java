package com.kleos.transfers.playerseason.repository;

import com.kleos.transfers.playerseason.entity.PlayerSeason;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    @Query("""
            select ps from PlayerSeason ps
            join fetch ps.season s
            join fetch ps.club
            where ps.player.id = :playerId
            order by s.startDate desc
            """)
    List<PlayerSeason> findHistoryByPlayerId(@Param("playerId") UUID playerId);

    @Query("""
            select ps from PlayerSeason ps
            join fetch ps.player
            where ps.club.id = :clubId and ps.season.id = :seasonId
            """)
    List<PlayerSeason> findByClubIdAndSeasonId(
            @Param("clubId") UUID clubId,
            @Param("seasonId") UUID seasonId
    );

    @Query("""
            select ps from PlayerSeason ps
            where ps.player.id = :playerId
              and ps.club.id = :clubId
              and ps.season.id = :seasonId
            """)
    Optional<PlayerSeason> findByPlayerIdAndClubIdAndSeasonId(
            @Param("playerId") UUID playerId,
            @Param("clubId") UUID clubId,
            @Param("seasonId") UUID seasonId
    );
}
