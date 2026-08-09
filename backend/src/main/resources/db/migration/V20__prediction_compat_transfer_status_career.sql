-- Dimensional compatibility scores on predictions (#50).
ALTER TABLE predictions
    ADD COLUMN compatibility_system NUMERIC(5, 2),
    ADD COLUMN compatibility_role NUMERIC(5, 2),
    ADD COLUMN compatibility_tempo NUMERIC(5, 2),
    ADD COLUMN compatibility_league NUMERIC(5, 2),
    ADD COLUMN compatibility_manager NUMERIC(5, 2);

ALTER TABLE predictions
    ADD CONSTRAINT chk_predictions_compat_system
        CHECK (compatibility_system IS NULL OR (compatibility_system >= 0 AND compatibility_system <= 100)),
    ADD CONSTRAINT chk_predictions_compat_role
        CHECK (compatibility_role IS NULL OR (compatibility_role >= 0 AND compatibility_role <= 100)),
    ADD CONSTRAINT chk_predictions_compat_tempo
        CHECK (compatibility_tempo IS NULL OR (compatibility_tempo >= 0 AND compatibility_tempo <= 100)),
    ADD CONSTRAINT chk_predictions_compat_league
        CHECK (compatibility_league IS NULL OR (compatibility_league >= 0 AND compatibility_league <= 100)),
    ADD CONSTRAINT chk_predictions_compat_manager
        CHECK (compatibility_manager IS NULL OR (compatibility_manager >= 0 AND compatibility_manager <= 100));

-- Transfer lifecycle status (#44): completed facts vs announced / rumoured.
ALTER TABLE transfers
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN source VARCHAR(64),
    ADD COLUMN notes VARCHAR(500);

ALTER TABLE transfers
    ALTER COLUMN uniqueness_key TYPE VARCHAR(240);

ALTER TABLE transfers
    DROP CONSTRAINT chk_transfers_type;

ALTER TABLE transfers
    ADD CONSTRAINT chk_transfers_type CHECK (
        type IN ('PERMANENT', 'LOAN', 'FREE', 'LOAN_RETURN')
    ),
    ADD CONSTRAINT chk_transfers_status CHECK (
        status IN ('COMPLETED', 'ANNOUNCED', 'RUMOURED')
    );

-- Rebuild uniqueness to include status so a rumour and a completed move can coexist.
UPDATE transfers
SET uniqueness_key = player_id::text
    || ':'
    || transfer_date::text
    || ':'
    || COALESCE(from_club_id::text, 'none')
    || ':'
    || COALESCE(to_club_id::text, 'none')
    || ':'
    || type
    || ':'
    || status
WHERE deleted_at IS NULL;

-- Curated / imported career league totals for all-time boards (#43).
CREATE TABLE league_career_totals (
    id UUID PRIMARY KEY,
    league_code VARCHAR(32) NOT NULL,
    metric VARCHAR(16) NOT NULL,
    rank INT NOT NULL,
    player_name VARCHAR(160) NOT NULL,
    total INT NOT NULL,
    player_id UUID REFERENCES players (id),
    source VARCHAR(120) NOT NULL,
    source_url VARCHAR(500),
    as_of_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_league_career_metric CHECK (metric IN ('GOALS', 'ASSISTS')),
    CONSTRAINT chk_league_career_rank CHECK (rank >= 1),
    CONSTRAINT chk_league_career_total CHECK (total >= 0)
);

CREATE UNIQUE INDEX uq_league_career_league_metric_rank
    ON league_career_totals (league_code, metric, rank)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_league_career_league_metric
    ON league_career_totals (league_code, metric)
    WHERE deleted_at IS NULL;
