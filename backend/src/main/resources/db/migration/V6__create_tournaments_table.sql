CREATE TABLE tournaments (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    name_normalized VARCHAR(160) NOT NULL,
    short_name VARCHAR(40) NOT NULL,
    confederation VARCHAR(16) NOT NULL,
    type VARCHAR(16) NOT NULL,
    country_code VARCHAR(3),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_tournaments_confederation CHECK (
        confederation IN ('UEFA', 'CONMEBOL', 'CONCACAF', 'CAF', 'AFC', 'OFC', 'FIFA')
    ),
    CONSTRAINT chk_tournaments_type CHECK (
        type IN ('LEAGUE', 'CUP', 'SUPER_CUP')
    )
);

-- Case-insensitive uniqueness via name_normalized.
-- Soft delete appends "#<id>" so the active name can be reused.
CREATE UNIQUE INDEX uq_tournaments_name_normalized
    ON tournaments (name_normalized);

CREATE INDEX idx_tournaments_name ON tournaments (name);

CREATE INDEX idx_tournaments_deleted_at ON tournaments (deleted_at);
