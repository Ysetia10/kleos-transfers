ALTER TABLE predictions
    ADD COLUMN predicted_minutes_low INTEGER,
    ADD COLUMN predicted_minutes_high INTEGER;

UPDATE predictions
SET predicted_minutes_low = predicted_minutes,
    predicted_minutes_high = predicted_minutes
WHERE predicted_minutes_low IS NULL;

ALTER TABLE predictions
    ALTER COLUMN predicted_minutes_low SET NOT NULL,
    ALTER COLUMN predicted_minutes_high SET NOT NULL;

ALTER TABLE predictions
    ADD CONSTRAINT chk_predictions_minutes_low CHECK (predicted_minutes_low >= 0),
    ADD CONSTRAINT chk_predictions_minutes_high CHECK (predicted_minutes_high >= 0),
    ADD CONSTRAINT chk_predictions_minutes_order CHECK (
        predicted_minutes_low <= predicted_minutes
        AND predicted_minutes <= predicted_minutes_high
    );
