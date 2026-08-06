package com.kleos.transfers.common.bulk;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds comparison keys used to detect duplicate identity records.
 *
 * <p>Keys are only compared against other keys built here, so the exact format
 * does not matter as long as both sides normalize identically.
 */
public final class NaturalKeys {

    private NaturalKeys() {
    }

    public static String of(Object... parts) {
        return Stream.of(parts)
                .map(NaturalKeys::normalize)
                .collect(Collectors.joining("|"));
    }

    private static String normalize(Object part) {
        if (part == null) {
            return "";
        }
        return part.toString().trim().toLowerCase(Locale.ROOT);
    }
}
