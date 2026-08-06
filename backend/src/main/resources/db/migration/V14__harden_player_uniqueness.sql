-- Harden Player uniqueness for real multi-season ingestion.
-- Active natural key: (full_name_normalized, date_of_birth, nationality).
-- Soft delete appends "#<id>" to full_name_normalized (and fbref_id when set).
-- Optional FBref player id is the preferred ingest match key.

ALTER TABLE players
    ADD COLUMN full_name_normalized VARCHAR(160);

UPDATE players
SET full_name_normalized = LOWER(TRIM(full_name))
WHERE full_name_normalized IS NULL;

ALTER TABLE players
    ALTER COLUMN full_name_normalized SET NOT NULL;

ALTER TABLE players
    ADD COLUMN fbref_id VARCHAR(40);

-- Height / foot are often unavailable from season-stat sources; keep optional.
ALTER TABLE players
    ALTER COLUMN height_cm DROP NOT NULL;

ALTER TABLE players
    ALTER COLUMN preferred_foot DROP NOT NULL;

CREATE UNIQUE INDEX uq_players_name_dob_nationality
    ON players (full_name_normalized, date_of_birth, nationality);

CREATE UNIQUE INDEX uq_players_fbref_id
    ON players (fbref_id);

CREATE INDEX idx_players_fbref_id ON players (fbref_id);
