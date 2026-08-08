-- Optional Wikimedia (or curated) media URLs for identity display.
-- Binary assets are not stored; only HTTPS URLs + attribution metadata.

ALTER TABLE players
    ADD COLUMN photo_url VARCHAR(1000),
    ADD COLUMN photo_attribution VARCHAR(500),
    ADD COLUMN photo_license VARCHAR(80),
    ADD COLUMN photo_source VARCHAR(40);

ALTER TABLE clubs
    ADD COLUMN crest_url VARCHAR(1000),
    ADD COLUMN crest_attribution VARCHAR(500),
    ADD COLUMN crest_license VARCHAR(80),
    ADD COLUMN crest_source VARCHAR(40);
