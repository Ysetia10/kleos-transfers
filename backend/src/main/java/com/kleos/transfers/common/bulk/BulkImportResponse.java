package com.kleos.transfers.common.bulk;

import java.util.List;

/**
 * Outcome of a bulk identity import, reporting per-row results.
 */
public record BulkImportResponse<T>(
        int requested,
        int createdCount,
        int skippedCount,
        int failedCount,
        List<T> created,
        List<BulkImportIssue> skipped,
        List<BulkImportIssue> failed
) {

    public static <T> BulkImportResponse<T> of(
            int requested,
            List<T> created,
            List<BulkImportIssue> skipped,
            List<BulkImportIssue> failed
    ) {
        return new BulkImportResponse<>(
                requested,
                created.size(),
                skipped.size(),
                failed.size(),
                created,
                skipped,
                failed
        );
    }

    /**
     * A row that was not created, identified by its position in the request.
     */
    public record BulkImportIssue(int index, String reference, String reason) {
    }
}
