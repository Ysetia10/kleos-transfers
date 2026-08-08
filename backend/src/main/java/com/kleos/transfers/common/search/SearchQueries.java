package com.kleos.transfers.common.search;

import java.util.Locale;

/**
 * Helpers for free-text repository search.
 */
public final class SearchQueries {

    private SearchQueries() {
    }

    /** Escapes {@code %}, {@code _}, and backslash for SQL {@code LIKE}. */
    public static String escapeLike(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    public static String normalize(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public static String upper(String raw) {
        return normalize(raw).toUpperCase(Locale.ROOT);
    }
}
