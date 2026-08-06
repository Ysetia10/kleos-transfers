-- Generated FBref keys (name + birth year + nation) exceed 40 characters.
ALTER TABLE players
    ALTER COLUMN fbref_id TYPE VARCHAR(120);

ALTER TABLE clubs
    ALTER COLUMN fbref_id TYPE VARCHAR(120);
