CREATE TABLE clubs (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    name_normalized VARCHAR(160) NOT NULL,
    short_name VARCHAR(40) NOT NULL,
    country_code VARCHAR(3) NOT NULL,
    founded_year INTEGER CHECK (
        founded_year IS NULL OR (founded_year BETWEEN 1800 AND 2100)
    ),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Case-insensitive uniqueness via name_normalized (lowercase display name).
-- Soft delete appends "#<id>" to name_normalized so the active name can be reused.
CREATE UNIQUE INDEX uq_clubs_name_country
    ON clubs (name_normalized, country_code);

CREATE INDEX idx_clubs_name ON clubs (name);

CREATE INDEX idx_clubs_deleted_at ON clubs (deleted_at);
