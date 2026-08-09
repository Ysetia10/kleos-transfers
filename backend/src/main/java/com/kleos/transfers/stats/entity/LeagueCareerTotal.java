package com.kleos.transfers.stats.entity;

import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.domain.CareerMetric;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.stats.domain.LeagueCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Curated career leaderboard row for a league (goals or assists).
 */
@Entity
@Table(name = "league_career_totals")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeagueCareerTotal extends IdentityEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "league_code", nullable = false, length = 32)
    private LeagueCode leagueCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CareerMetric metric;

    @Column(nullable = false)
    private Integer rank;

    @Column(name = "player_name", nullable = false, length = 160)
    private String playerName;

    @Column(nullable = false)
    private Integer total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @Column(nullable = false, length = 120)
    private String source;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    public LeagueCareerTotal(
            LeagueCode leagueCode,
            CareerMetric metric,
            Integer rank,
            String playerName,
            Integer total,
            Player player,
            String source,
            String sourceUrl,
            LocalDate asOfDate
    ) {
        this.leagueCode = leagueCode;
        this.metric = metric;
        this.rank = rank;
        this.playerName = playerName;
        this.total = total;
        this.player = player;
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.asOfDate = asOfDate;
    }

    public void bindPlayer(Player player) {
        this.player = player;
    }
}
