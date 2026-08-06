CREATE TABLE seasons (
    id UUID PRIMARY KEY,
    label VARCHAR(20) NOT NULL,
    label_normalized VARCHAR(60) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_seasons_date_range CHECK (end_date > start_date)
);

-- Case-insensitive uniqueness via label_normalized.
-- Soft delete appends "#<id>" so the active label can be reused.
CREATE UNIQUE INDEX uq_seasons_label_normalized
    ON seasons (label_normalized);

CREATE INDEX idx_seasons_start_date ON seasons (start_date);

CREATE INDEX idx_seasons_deleted_at ON seasons (deleted_at);
