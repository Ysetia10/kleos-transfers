-- FBref season tables often expose birth year only. Ingest stores YYYY-07-01 as a
-- mid-year age anchor; precision YEAR tells the API/UI to display the year alone.
ALTER TABLE players
    ADD COLUMN date_of_birth_precision VARCHAR(4) NOT NULL DEFAULT 'DAY'
        CHECK (date_of_birth_precision IN ('DAY', 'YEAR'));

-- Existing FBref-ingested rows all used the July-1 year anchor.
UPDATE players
SET date_of_birth_precision = 'YEAR'
WHERE EXTRACT(MONTH FROM date_of_birth) = 7
  AND EXTRACT(DAY FROM date_of_birth) = 1;
