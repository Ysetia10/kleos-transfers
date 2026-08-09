-- ManagerSeason tactical context for club fit / recruitment signals (#48).
ALTER TABLE manager_seasons
    ADD COLUMN tactical_system VARCHAR(20),
    ADD COLUMN tempo VARCHAR(10),
    ADD COLUMN youth_minutes_pct NUMERIC(5, 2);

ALTER TABLE manager_seasons
    ADD CONSTRAINT chk_manager_seasons_tactical_system
        CHECK (tactical_system IS NULL OR tactical_system IN (
            'POSSESSION', 'TRANSITION', 'DIRECT', 'BALANCED'
        ));

ALTER TABLE manager_seasons
    ADD CONSTRAINT chk_manager_seasons_tempo
        CHECK (tempo IS NULL OR tempo IN ('LOW', 'MEDIUM', 'HIGH'));

ALTER TABLE manager_seasons
    ADD CONSTRAINT chk_manager_seasons_youth_minutes_pct
        CHECK (youth_minutes_pct IS NULL OR (youth_minutes_pct >= 0 AND youth_minutes_pct <= 100));
