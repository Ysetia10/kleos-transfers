CREATE TABLE club_seasons (
    id UUID PRIMARY KEY,
    club_id UUID NOT NULL REFERENCES clubs (id),
    season_id UUID NOT NULL REFERENCES seasons (id),
    tournament_id UUID NOT NULL REFERENCES tournaments (id),
    -- Active rows use "{clubId}:{seasonId}"; soft delete appends "#{id}" to free the slot.
    uniqueness_key VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX uq_club_seasons_uniqueness_key
    ON club_seasons (uniqueness_key);

CREATE INDEX idx_club_seasons_club_id ON club_seasons (club_id);

CREATE INDEX idx_club_seasons_season_id ON club_seasons (season_id);

CREATE INDEX idx_club_seasons_tournament_id ON club_seasons (tournament_id);

CREATE INDEX idx_club_seasons_deleted_at ON club_seasons (deleted_at);
