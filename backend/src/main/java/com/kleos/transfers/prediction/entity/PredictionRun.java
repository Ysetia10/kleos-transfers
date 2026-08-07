package com.kleos.transfers.prediction.entity;

import com.kleos.transfers.common.entity.IdentityEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * Audit wrapper for one execution of the prediction engine.
 *
 * <p>Holds the model version so later algorithm changes do not rewrite past outputs.
 */
@Entity
@Table(name = "prediction_runs")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PredictionRun extends IdentityEntity {

    public static final String MODEL_VERSION_V0 = "v0-heuristic";

    /** Calibrated minutes weighting / softer squad competition vs {@link #MODEL_VERSION_V0}. */
    public static final String MODEL_VERSION_V0_1 = "v0.1-heuristic";

    /** Adds a goalkeeper starter/backup minutes pathway on top of {@link #MODEL_VERSION_V0_1}. */
    public static final String MODEL_VERSION_V0_2 = "v0.2-heuristic";

    @Column(name = "model_version", nullable = false, length = 40)
    private String modelVersion;

    @Column(length = 255)
    private String note;

    public PredictionRun(String modelVersion, String note) {
        this.modelVersion = modelVersion;
        this.note = note == null || note.isBlank() ? null : note.trim();
    }
}
