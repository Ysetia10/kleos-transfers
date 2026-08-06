CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    player_id UUID NOT NULL REFERENCES players (id),
    from_club_id UUID REFERENCES clubs (id),
    to_club_id UUID REFERENCES clubs (id),
    season_id UUID NOT NULL REFERENCES seasons (id),
    transfer_date DATE NOT NULL,
    fee_eur NUMERIC(14, 2) CHECK (fee_eur IS NULL OR fee_eur >= 0),
    type VARCHAR(16) NOT NULL,
    -- Active rows use "{playerId}:{date}:{from}:{to}:{type}"; soft delete appends "#{id}".
    uniqueness_key VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_transfers_type CHECK (
        type IN ('PERMANENT', 'LOAN', 'FREE', 'LOAN_RETURN')
    ),
    CONSTRAINT chk_transfers_has_club CHECK (
        from_club_id IS NOT NULL OR to_club_id IS NOT NULL
    ),
    CONSTRAINT chk_transfers_different_clubs CHECK (
        from_club_id IS NULL
        OR to_club_id IS NULL
        OR from_club_id <> to_club_id
    )
);

CREATE UNIQUE INDEX uq_transfers_uniqueness_key
    ON transfers (uniqueness_key);

CREATE INDEX idx_transfers_player_id ON transfers (player_id);

CREATE INDEX idx_transfers_from_club_id ON transfers (from_club_id);

CREATE INDEX idx_transfers_to_club_id ON transfers (to_club_id);

CREATE INDEX idx_transfers_season_id ON transfers (season_id);

CREATE INDEX idx_transfers_transfer_date ON transfers (transfer_date);

CREATE INDEX idx_transfers_deleted_at ON transfers (deleted_at);
