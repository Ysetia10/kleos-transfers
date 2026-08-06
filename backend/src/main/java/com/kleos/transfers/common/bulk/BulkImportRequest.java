package com.kleos.transfers.common.bulk;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Envelope for bulk identity imports.
 *
 * <p>Items are intentionally not cascade-validated so that one invalid row does not
 * reject the whole batch; each item is validated individually by {@link BulkImporter}.
 */
public record BulkImportRequest<T>(
        @NotEmpty @Size(max = BulkImportRequest.MAX_ITEMS) List<T> items
) {

    public static final int MAX_ITEMS = 500;
}
