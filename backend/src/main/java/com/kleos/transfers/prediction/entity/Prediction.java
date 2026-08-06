package com.kleos.transfers.prediction.entity;

import com.kleos.transfers.club.entity.Club;
import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.player.entity.Player;
import com.kleos.transfers.season.entity.Season;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * One transfer what-if: predicted performance of a player at a target club for a season.
 *
 * <p>All metric fields live on this row so a prediction is one product object, not a
 * scatter of metric tables. Factor-level explanations are child rows.
 */
@Entity
@Table(name = "predictions")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Prediction extends IdentityEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private PredictionRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_club_id", nullable = false)
    private Club targetClub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "predicted_minutes", nullable = false)
    private Integer predictedMinutes;

    @Column(name = "predicted_goals", nullable = false, precision = 6, scale = 2)
    private BigDecimal predictedGoals;

    @Column(name = "predicted_assists", nullable = false, precision = 6, scale = 2)
    private BigDecimal predictedAssists;

    @Column(name = "predicted_xg", nullable = false, precision = 6, scale = 2)
    private BigDecimal predictedXg;

    @Column(name = "predicted_xa", nullable = false, precision = 6, scale = 2)
    private BigDecimal predictedXa;

    @Column(name = "predicted_market_value_eur", precision = 14, scale = 2)
    private BigDecimal predictedMarketValueEur;

    @Column(name = "compatibility_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal compatibilityScore;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @OneToMany(mappedBy = "prediction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PredictionExplanation> explanations = new ArrayList<>();

    @OneToOne(mappedBy = "prediction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PredictionEvaluation evaluation;

    public Prediction(
            PredictionRun run,
            Player player,
            Club targetClub,
            Season season,
            Integer predictedMinutes,
            BigDecimal predictedGoals,
            BigDecimal predictedAssists,
            BigDecimal predictedXg,
            BigDecimal predictedXa,
            BigDecimal predictedMarketValueEur,
            BigDecimal compatibilityScore,
            BigDecimal confidenceScore
    ) {
        this.run = run;
        this.player = player;
        this.targetClub = targetClub;
        this.season = season;
        this.predictedMinutes = predictedMinutes;
        this.predictedGoals = predictedGoals;
        this.predictedAssists = predictedAssists;
        this.predictedXg = predictedXg;
        this.predictedXa = predictedXa;
        this.predictedMarketValueEur = predictedMarketValueEur;
        this.compatibilityScore = compatibilityScore;
        this.confidenceScore = confidenceScore;
    }

    public void addExplanation(PredictionExplanation explanation) {
        explanations.add(explanation);
    }

    public void attachEvaluation(PredictionEvaluation evaluation) {
        this.evaluation = evaluation;
    }
}
