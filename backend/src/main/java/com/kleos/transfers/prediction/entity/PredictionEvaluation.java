package com.kleos.transfers.prediction.entity;

import com.kleos.transfers.common.entity.IdentityEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Post-season comparison of a prediction against observed PlayerSeason outcomes.
 */
@Entity
@Table(name = "prediction_evaluations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PredictionEvaluation extends IdentityEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false, unique = true)
    private Prediction prediction;

    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    @Column(name = "actual_goals")
    private Integer actualGoals;

    @Column(name = "actual_assists")
    private Integer actualAssists;

    @Column(name = "minutes_error")
    private Integer minutesError;

    @Column(name = "goals_error", precision = 6, scale = 2)
    private BigDecimal goalsError;

    @Column(name = "assists_error", precision = 6, scale = 2)
    private BigDecimal assistsError;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    public PredictionEvaluation(
            Prediction prediction,
            Integer actualMinutes,
            Integer actualGoals,
            Integer actualAssists,
            Integer minutesError,
            BigDecimal goalsError,
            BigDecimal assistsError,
            Instant evaluatedAt
    ) {
        this.prediction = prediction;
        this.actualMinutes = actualMinutes;
        this.actualGoals = actualGoals;
        this.actualAssists = actualAssists;
        this.minutesError = minutesError;
        this.goalsError = goalsError;
        this.assistsError = assistsError;
        this.evaluatedAt = evaluatedAt;
    }
}
