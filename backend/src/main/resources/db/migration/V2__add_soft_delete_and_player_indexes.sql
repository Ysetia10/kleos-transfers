-- Soft-delete support for identity entities + list sort index.
ALTER TABLE players
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_players_full_name ON players (full_name);

CREATE INDEX idx_players_deleted_at ON players (deleted_at);
