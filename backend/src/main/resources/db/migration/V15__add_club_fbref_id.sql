-- Optional FBref squad/club id for stable club matching during ingestion.
-- Soft delete appends "#<id>" to fbref_id when set so the slot can be reused.

ALTER TABLE clubs
    ADD COLUMN fbref_id VARCHAR(40);

CREATE UNIQUE INDEX uq_clubs_fbref_id
    ON clubs (fbref_id);

CREATE INDEX idx_clubs_fbref_id ON clubs (fbref_id);
