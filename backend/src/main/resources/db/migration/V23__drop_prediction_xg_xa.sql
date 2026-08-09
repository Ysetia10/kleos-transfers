-- Predictions focus on minutes / goals / assists / value; drop expected-stat projections.
ALTER TABLE predictions
    DROP COLUMN IF EXISTS predicted_xg,
    DROP COLUMN IF EXISTS predicted_xa;

ALTER TABLE prediction_evaluations
    DROP COLUMN IF EXISTS actual_xg,
    DROP COLUMN IF EXISTS actual_xa,
    DROP COLUMN IF EXISTS xg_error,
    DROP COLUMN IF EXISTS xa_error;
