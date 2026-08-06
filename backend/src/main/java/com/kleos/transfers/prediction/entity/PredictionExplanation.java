package com.kleos.transfers.prediction.entity;

import com.kleos.transfers.common.entity.IdentityEntity;
import com.kleos.transfers.domain.ExplanationDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One human-readable factor behind a prediction (the product's explainability surface).
 *
 * <p>No soft delete — explanations are owned by the parent prediction lifecycle.
 */
@Entity
@Table(name = "prediction_explanations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PredictionExplanation extends IdentityEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prediction_id", nullable = false)
    private Prediction prediction;

    @Column(name = "factor_code", nullable = false, length = 40)
    private String factorCode;

    @Column(nullable = false, length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExplanationDirection direction;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal impact;

    @Column(nullable = false, length = 500)
    private String detail;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public PredictionExplanation(
            Prediction prediction,
            String factorCode,
            String label,
            ExplanationDirection direction,
            BigDecimal impact,
            String detail,
            Integer sortOrder
    ) {
        this.prediction = prediction;
        this.factorCode = factorCode;
        this.label = label;
        this.direction = direction;
        this.impact = impact;
        this.detail = detail;
        this.sortOrder = sortOrder;
    }
}
