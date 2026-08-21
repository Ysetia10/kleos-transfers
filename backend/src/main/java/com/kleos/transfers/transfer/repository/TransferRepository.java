package com.kleos.transfers.transfer.repository;

import com.kleos.transfers.domain.TransferStatus;
import com.kleos.transfers.transfer.entity.Transfer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persistence access for transfer records.
 */
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    /**
     * Loads active transfers whose uniqueness keys match any of the given keys.
     */
    @Query("select t from Transfer t where t.uniquenessKey in :keys")
    List<Transfer> findAllByUniquenessKeyIn(@Param("keys") Collection<String> keys);

    Page<Transfer> findByStatus(TransferStatus status, Pageable pageable);

    Page<Transfer> findBySeason_Id(UUID seasonId, Pageable pageable);

    Page<Transfer> findByStatusAndSeason_Id(TransferStatus status, UUID seasonId, Pageable pageable);

    /**
     * Confirmed or announced moves involving a club in a season (outs and ins).
     * Used to project an empty upcoming roster from the prior season.
     */
    @Query("""
            select t from Transfer t
            join fetch t.player
            left join fetch t.fromClub
            left join fetch t.toClub
            where t.season.id = :seasonId
              and (t.fromClub.id = :clubId or t.toClub.id = :clubId)
              and t.status in :statuses
            """)
    List<Transfer> findBySeasonIdAndClubIdAndStatusIn(
            @Param("seasonId") UUID seasonId,
            @Param("clubId") UUID clubId,
            @Param("statuses") Collection<TransferStatus> statuses
    );

    /**
     * Inbound (to-club) moves for players — used to refresh "latest club" after window signings.
     */
    @Query("""
            select t from Transfer t
            join fetch t.player
            join fetch t.toClub
            left join fetch t.fromClub
            join fetch t.season
            where t.player.id in :playerIds
              and t.toClub is not null
              and t.status in :statuses
            """)
    List<Transfer> findInboundByPlayerIdInAndStatusIn(
            @Param("playerIds") Collection<UUID> playerIds,
            @Param("statuses") Collection<TransferStatus> statuses
    );
    /**
     * Confirmed or announced moves for a set of players in one season (any club).
     * Used to spot prior-squad members leaving even when {@code fromClub} is missing.
     */
    @Query("""
            select t from Transfer t
            join fetch t.player
            left join fetch t.fromClub
            left join fetch t.toClub
            where t.season.id = :seasonId
              and t.player.id in :playerIds
              and t.status in :statuses
            """)
    List<Transfer> findBySeasonIdAndPlayerIdInAndStatusIn(
            @Param("seasonId") UUID seasonId,
            @Param("playerIds") Collection<UUID> playerIds,
            @Param("statuses") Collection<TransferStatus> statuses
    );
}
