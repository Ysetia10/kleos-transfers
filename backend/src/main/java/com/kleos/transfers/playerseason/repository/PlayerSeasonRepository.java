package com.kleos.transfers.playerseason.repository;

import com.kleos.transfers.player.dto.LatestClubView;
import com.kleos.transfers.playerseason.entity.PlayerSeason;
import java.time.LocalDate;
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

    /**
     * Player history strictly before {@code asOf} (typically the target season start date).
     * Used so predictions / backtests never see same-season outcomes.
     */
    @Query("""
            select ps from PlayerSeason ps
            join fetch ps.season s
            join fetch ps.club
            where ps.player.id = :playerId
              and s.startDate < :asOf
            order by s.startDate desc
            """)
    List<PlayerSeason> findHistoryByPlayerIdBefore(
            @Param("playerId") UUID playerId,
            @Param("asOf") LocalDate asOf
    );

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

    /**
     * Latest club per player (most recent season start, then highest minutes).
     */
    @Query(
            value = """
                    SELECT DISTINCT ON (ps.player_id)
                           ps.player_id AS "playerId",
                           c.id AS "clubId",
                           c.name AS "clubName",
                           s.label AS "seasonLabel"
                    FROM player_seasons ps
                    JOIN clubs c ON c.id = ps.club_id
                    JOIN seasons s ON s.id = ps.season_id
                    WHERE ps.deleted_at IS NULL
                      AND ps.player_id IN (:playerIds)
                    ORDER BY ps.player_id, s.start_date DESC, ps.minutes_played DESC
                    """,
            nativeQuery = true
    )
    List<LatestClubView> findLatestClubsByPlayerIds(@Param("playerIds") Collection<UUID> playerIds);

    @Query(
            value = """
                    SELECT p.id AS playerId,
                           p.full_name AS playerName,
                           CAST(NULL AS uuid) AS clubId,
                           CAST(NULL AS text) AS clubName,
                           SUM(ps.goals) AS goals,
                           SUM(ps.assists) AS assists,
                           SUM(ps.appearances) AS appearances,
                           SUM(ps.minutes_played) AS minutesPlayed,
                           COUNT(DISTINCT ps.season_id) AS seasonsPlayed
                    FROM player_seasons ps
                    JOIN players p ON p.id = ps.player_id
                    JOIN club_seasons cs
                      ON cs.club_id = ps.club_id
                     AND cs.season_id = ps.season_id
                     AND cs.deleted_at IS NULL
                    JOIN tournaments t ON t.id = cs.tournament_id AND t.deleted_at IS NULL
                    WHERE ps.deleted_at IS NULL
                      AND t.name = :tournamentName
                      AND ps.season_id = :seasonId
                    GROUP BY p.id, p.full_name
                    ORDER BY SUM(ps.goals) DESC, SUM(ps.assists) DESC, p.full_name
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<Object[]> findSeasonGoalLeaders(
            @Param("tournamentName") String tournamentName,
            @Param("seasonId") UUID seasonId,
            @Param("limit") int limit
    );

    @Query(
            value = """
                    SELECT p.id AS playerId,
                           p.full_name AS playerName,
                           CAST(NULL AS uuid) AS clubId,
                           CAST(NULL AS text) AS clubName,
                           SUM(ps.goals) AS goals,
                           SUM(ps.assists) AS assists,
                           SUM(ps.appearances) AS appearances,
                           SUM(ps.minutes_played) AS minutesPlayed,
                           COUNT(DISTINCT ps.season_id) AS seasonsPlayed
                    FROM player_seasons ps
                    JOIN players p ON p.id = ps.player_id
                    JOIN club_seasons cs
                      ON cs.club_id = ps.club_id
                     AND cs.season_id = ps.season_id
                     AND cs.deleted_at IS NULL
                    JOIN tournaments t ON t.id = cs.tournament_id AND t.deleted_at IS NULL
                    WHERE ps.deleted_at IS NULL
                      AND t.name = :tournamentName
                      AND ps.season_id = :seasonId
                    GROUP BY p.id, p.full_name
                    ORDER BY SUM(ps.assists) DESC, SUM(ps.goals) DESC, p.full_name
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<Object[]> findSeasonAssistLeaders(
            @Param("tournamentName") String tournamentName,
            @Param("seasonId") UUID seasonId,
            @Param("limit") int limit
    );

    @Query(
            value = """
                    SELECT p.id AS playerId,
                           p.full_name AS playerName,
                           CAST(NULL AS uuid) AS clubId,
                           CAST(NULL AS text) AS clubName,
                           SUM(ps.goals) AS goals,
                           SUM(ps.assists) AS assists,
                           SUM(ps.appearances) AS appearances,
                           SUM(ps.minutes_played) AS minutesPlayed,
                           COUNT(DISTINCT ps.season_id) AS seasonsPlayed
                    FROM player_seasons ps
                    JOIN players p ON p.id = ps.player_id
                    JOIN club_seasons cs
                      ON cs.club_id = ps.club_id
                     AND cs.season_id = ps.season_id
                     AND cs.deleted_at IS NULL
                    JOIN tournaments t ON t.id = cs.tournament_id AND t.deleted_at IS NULL
                    WHERE ps.deleted_at IS NULL
                      AND t.name = :tournamentName
                    GROUP BY p.id, p.full_name
                    ORDER BY SUM(ps.goals) DESC, SUM(ps.assists) DESC, p.full_name
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<Object[]> findAllTimeGoalLeaders(
            @Param("tournamentName") String tournamentName,
            @Param("limit") int limit
    );

    @Query(
            value = """
                    SELECT p.id AS playerId,
                           p.full_name AS playerName,
                           CAST(NULL AS uuid) AS clubId,
                           CAST(NULL AS text) AS clubName,
                           SUM(ps.goals) AS goals,
                           SUM(ps.assists) AS assists,
                           SUM(ps.appearances) AS appearances,
                           SUM(ps.minutes_played) AS minutesPlayed,
                           COUNT(DISTINCT ps.season_id) AS seasonsPlayed
                    FROM player_seasons ps
                    JOIN players p ON p.id = ps.player_id
                    JOIN club_seasons cs
                      ON cs.club_id = ps.club_id
                     AND cs.season_id = ps.season_id
                     AND cs.deleted_at IS NULL
                    JOIN tournaments t ON t.id = cs.tournament_id AND t.deleted_at IS NULL
                    WHERE ps.deleted_at IS NULL
                      AND t.name = :tournamentName
                    GROUP BY p.id, p.full_name
                    ORDER BY SUM(ps.assists) DESC, SUM(ps.goals) DESC, p.full_name
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<Object[]> findAllTimeAssistLeaders(
            @Param("tournamentName") String tournamentName,
            @Param("limit") int limit
    );
}
