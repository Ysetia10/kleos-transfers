-- Prediction layer: runs, scenario outputs, factor explanations, and post-season evaluations.
-- Predictions are Kleos intelligence — kept separate from identity and historical facts.

CREATE TABLE prediction_runs (
    id UUID PRIMARY KEY,
    model_version VARCHAR(40) NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_prediction_runs_created_at ON prediction_runs (created_at);

CREATE INDEX idx_prediction_runs_deleted_at ON prediction_runs (deleted_at);

CREATE TABLE predictions (
    id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES prediction_runs (id),
    player_id UUID NOT NULL REFERENCES players (id),
    target_club_id UUID NOT NULL REFERENCES clubs (id),
    season_id UUID NOT NULL REFERENCES seasons (id),
    predicted_minutes INTEGER NOT NULL CHECK (predicted_minutes >= 0),
    predicted_goals NUMERIC(6, 2) NOT NULL CHECK (predicted_goals >= 0),
    predicted_assists NUMERIC(6, 2) NOT NULL CHECK (predicted_assists >= 0),
    predicted_xg NUMERIC(6, 2) NOT NULL CHECK (predicted_xg >= 0),
    predicted_xa NUMERIC(6, 2) NOT NULL CHECK (predicted_xa >= 0),
    predicted_market_value_eur NUMERIC(14, 2) CHECK (
        predicted_market_value_eur IS NULL OR predicted_market_value_eur >= 0
    ),
    compatibility_score NUMERIC(5, 2) NOT NULL CHECK (
        compatibility_score >= 0 AND compatibility_score <= 100
    ),
    confidence_score NUMERIC(5, 2) NOT NULL CHECK (
        confidence_score >= 0 AND confidence_score <= 100
    ),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_predictions_run_id ON predictions (run_id);

CREATE INDEX idx_predictions_player_id ON predictions (player_id);

CREATE INDEX idx_predictions_target_club_id ON predictions (target_club_id);

CREATE INDEX idx_predictions_season_id ON predictions (season_id);

CREATE INDEX idx_predictions_deleted_at ON predictions (deleted_at);

CREATE TABLE prediction_explanations (
    id UUID PRIMARY KEY,
    prediction_id UUID NOT NULL REFERENCES predictions (id),
    factor_code VARCHAR(40) NOT NULL,
    label VARCHAR(120) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    impact NUMERIC(5, 2) NOT NULL,
    detail VARCHAR(500) NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_prediction_explanations_direction CHECK (
        direction IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')
    )
);

CREATE INDEX idx_prediction_explanations_prediction_id
    ON prediction_explanations (prediction_id);

CREATE TABLE prediction_evaluations (
    id UUID PRIMARY KEY,
    prediction_id UUID NOT NULL UNIQUE REFERENCES predictions (id),
    actual_minutes INTEGER CHECK (actual_minutes IS NULL OR actual_minutes >= 0),
    actual_goals INTEGER CHECK (actual_goals IS NULL OR actual_goals >= 0),
    actual_assists INTEGER CHECK (actual_assists IS NULL OR actual_assists >= 0),
    actual_xg NUMERIC(8, 2) CHECK (actual_xg IS NULL OR actual_xg >= 0),
    actual_xa NUMERIC(8, 2) CHECK (actual_xa IS NULL OR actual_xa >= 0),
    minutes_error INTEGER,
    goals_error NUMERIC(6, 2),
    assists_error NUMERIC(6, 2),
    xg_error NUMERIC(6, 2),
    xa_error NUMERIC(6, 2),
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_prediction_evaluations_prediction_id
    ON prediction_evaluations (prediction_id);
