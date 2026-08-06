package com.kleos.transfers.common.test;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Hard-deletes rows for integration tests, including soft-deleted records
 * that JPA {@code @SQLRestriction} would otherwise hide from {@code deleteAllInBatch}.
 *
 * <p>Delete order respects foreign keys: historical tables first, then identity.
 */
public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void clearAll(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("delete from player_seasons");
        jdbcTemplate.update("delete from manager_seasons");
        jdbcTemplate.update("delete from club_seasons");
        jdbcTemplate.update("delete from players");
        jdbcTemplate.update("delete from managers");
        jdbcTemplate.update("delete from clubs");
        jdbcTemplate.update("delete from seasons");
        jdbcTemplate.update("delete from tournaments");
    }
}
