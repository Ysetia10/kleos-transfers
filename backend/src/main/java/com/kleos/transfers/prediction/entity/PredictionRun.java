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

    /** Adds dimensional compatibility (system/role/tempo/league/manager) on top of {@link #MODEL_VERSION_V0_2}. */
    public static final String MODEL_VERSION_V0_3 = "v0.3-heuristic";

    /** Hardens GK winner-take-most rules from club-changer backtests (incumbent stays → bench; vacated shirt → starter). */
    public static final String MODEL_VERSION_V0_4 = "v0.4-heuristic";

    /** Outfield vacancy/established floors + injury ingest readiness on top of {@link #MODEL_VERSION_V0_4}. */
    public static final String MODEL_VERSION_V0_5 = "v0.5-heuristic";

    /**
     * Broader vacancy detection (transfer outs without fromClub + open-depth walk-ins), recovered-injury
     * dampening, and sharper GK open-shirt / takeover edges on top of {@link #MODEL_VERSION_V0_5}.
     */
    public static final String MODEL_VERSION_V0_6 = "v0.6-heuristic";

    /**
     * Exact-role competition gate trusts lateral enrichment (RB/LB/CAM/…) without requiring a large
     * share of the whole outfield — PulseLive/FBref correctly leave many players as CB/CM/ST.
     */
    public static final String MODEL_VERSION_V0_7 = "v0.7-heuristic";

    /**
     * Free-agent / release vacancy coverage for GK (and outfield) plus a higher settled-#1 bar so
     * declining keepers do not falsely bench starter-level arrivals.
     */
    public static final String MODEL_VERSION_V0_8 = "v0.8-heuristic";

    /**
     * Coarse subject roles stay on line competition (fixes CB-tagged full-backs), softer exact-slot
     * floors for flanks, open-shirt GK path when no prior keeper exists, and wider arrival floors.
     */
    public static final String MODEL_VERSION_V0_9 = "v0.9-heuristic";

    @Column(name = "model_version", nullable = false, length = 40)
    private String modelVersion;

    @Column(length = 255)
    private String note;

    public PredictionRun(String modelVersion, String note) {
        this.modelVersion = modelVersion;
        this.note = note == null || note.isBlank() ? null : note.trim();
    }
}
