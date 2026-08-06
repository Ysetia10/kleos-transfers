CREATE TABLE player_seasons (
    id UUID PRIMARY KEY,
    player_id UUID NOT NULL REFERENCES players (id),
    club_id UUID NOT NULL REFERENCES clubs (id),
    season_id UUID NOT NULL REFERENCES seasons (id),
    appearances INTEGER NOT NULL CHECK (appearances >= 0),
    minutes_played INTEGER NOT NULL CHECK (minutes_played >= 0),
    goals INTEGER NOT NULL CHECK (goals >= 0),
    assists INTEGER NOT NULL CHECK (assists >= 0),
    xg NUMERIC(8, 2) NOT NULL CHECK (xg >= 0),
    xa NUMERIC(8, 2) NOT NULL CHECK (xa >= 0),
    primary_position VARCHAR(3) NOT NULL,
    -- Active rows use "{playerId}:{clubId}:{seasonId}"; soft delete appends "#{id}".
    uniqueness_key VARCHAR(160) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_player_seasons_minutes_vs_apps CHECK (
        appearances = 0 OR minutes_played >= appearances
    )
);

CREATE UNIQUE INDEX uq_player_seasons_uniqueness_key
    ON player_seasons (uniqueness_key);

CREATE INDEX idx_player_seasons_player_id ON player_seasons (player_id);

CREATE INDEX idx_player_seasons_club_id ON player_seasons (club_id);

CREATE INDEX idx_player_seasons_season_id ON player_seasons (season_id);

CREATE INDEX idx_player_seasons_deleted_at ON player_seasons (deleted_at);
