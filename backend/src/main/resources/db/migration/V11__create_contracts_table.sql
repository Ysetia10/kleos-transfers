CREATE TABLE contracts (
    id UUID PRIMARY KEY,
    player_id UUID NOT NULL REFERENCES players (id),
    club_id UUID NOT NULL REFERENCES clubs (id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    release_clause_eur NUMERIC(14, 2) CHECK (release_clause_eur IS NULL OR release_clause_eur >= 0),
    -- Active rows use "{playerId}:{clubId}:{startDate}"; soft delete appends "#{id}".
    uniqueness_key VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_contracts_date_range CHECK (end_date > start_date)
);

CREATE UNIQUE INDEX uq_contracts_uniqueness_key
    ON contracts (uniqueness_key);

CREATE INDEX idx_contracts_player_id ON contracts (player_id);

CREATE INDEX idx_contracts_club_id ON contracts (club_id);

CREATE INDEX idx_contracts_end_date ON contracts (end_date);

CREATE INDEX idx_contracts_deleted_at ON contracts (deleted_at);
