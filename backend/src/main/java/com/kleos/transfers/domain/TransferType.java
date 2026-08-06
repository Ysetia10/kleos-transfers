package com.kleos.transfers.domain;

/**
 * How a player moved between clubs (or into/out of free agency).
 */
public enum TransferType {
    PERMANENT,
    LOAN,
    FREE,
    LOAN_RETURN
}
