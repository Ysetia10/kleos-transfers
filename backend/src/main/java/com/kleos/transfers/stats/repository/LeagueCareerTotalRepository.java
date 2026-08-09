package com.kleos.transfers.stats.repository;

import com.kleos.transfers.domain.CareerMetric;
import com.kleos.transfers.stats.domain.LeagueCode;
import com.kleos.transfers.stats.entity.LeagueCareerTotal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for curated league career leaderboards.
 */
public interface LeagueCareerTotalRepository extends JpaRepository<LeagueCareerTotal, UUID> {

    List<LeagueCareerTotal> findByLeagueCodeAndMetricOrderByRankAsc(
            LeagueCode leagueCode,
            CareerMetric metric,
            Pageable pageable
    );

    boolean existsByLeagueCode(LeagueCode leagueCode);
}
