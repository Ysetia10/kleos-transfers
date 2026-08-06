package com.kleos.transfers.common.validation;

import java.time.LocalDate;

/**
 * Marker for request payloads that expose a start/end date range.
 */
public interface DateRangeRequest {

    LocalDate startDate();

    LocalDate endDate();
}
