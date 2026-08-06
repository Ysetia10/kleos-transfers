CREATE TABLE injuries (
    id UUID PRIMARY KEY,
    player_id UUID NOT NULL REFERENCES players (id),
    injury_type VARCHAR(80) NOT NULL,
    injury_type_normalized VARCHAR(80) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    start_date DATE NOT NULL,
    -- NULL means the player is still unavailable.
    end_date DATE,
    -- Active rows use "{playerId}:{startDate}:{injuryTypeNormalized}"; soft delete appends "#{id}".
    uniqueness_key VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_injuries_severity CHECK (
        severity IN ('MINOR', 'MODERATE', 'SEVERE')
    ),
    CONSTRAINT chk_injuries_date_range CHECK (
        end_date IS NULL OR end_date >= start_date
    )
);

CREATE UNIQUE INDEX uq_injuries_uniqueness_key
    ON injuries (uniqueness_key);

CREATE INDEX idx_injuries_player_id ON injuries (player_id);

CREATE INDEX idx_injuries_start_date ON injuries (start_date);

CREATE INDEX idx_injuries_deleted_at ON injuries (deleted_at);
