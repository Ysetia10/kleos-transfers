package com.kleos.transfers.common.bulk;

import java.util.List;
import java.util.Set;

/**
 * Module-specific behavior required to bulk import one identity type.
 *
 * @param <C> create-request type
 * @param <R> API response type
 */
public interface BulkImportSpec<C, R> {

    /** Natural key used to detect rows that already exist. */
    String naturalKey(C request);

    /** Human-readable label reported back for skipped or failed rows. */
    String reference(C request);

    /** Natural keys of active records that already exist for the given requests. */
    Set<String> findExistingKeys(List<C> requests);

    /** Persists the accepted requests and returns their API representations. */
    List<R> persist(List<C> accepted);
}
