CREATE TABLE manager_seasons (
    id UUID PRIMARY KEY,
    manager_id UUID NOT NULL REFERENCES managers (id),
    club_id UUID NOT NULL REFERENCES clubs (id),
    season_id UUID NOT NULL REFERENCES seasons (id),
    -- Active rows use "{managerId}:{clubId}:{seasonId}"; soft delete appends "#{id}".
    uniqueness_key VARCHAR(160) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_manager_seasons_uniqueness_key
    ON manager_seasons (uniqueness_key);

CREATE INDEX idx_manager_seasons_manager_id ON manager_seasons (manager_id);

CREATE INDEX idx_manager_seasons_club_id ON manager_seasons (club_id);

CREATE INDEX idx_manager_seasons_season_id ON manager_seasons (season_id);

CREATE INDEX idx_manager_seasons_deleted_at ON manager_seasons (deleted_at);
