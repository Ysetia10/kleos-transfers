package com.kleos.transfers.domain;

/**
 * Lifecycle of a transfer record — facts versus market signals.
 */
public enum TransferStatus {
    /** Confirmed completed move (or inferred from consecutive club seasons). */
    COMPLETED,
    /** Publicly announced but not yet treated as fully settled. */
    ANNOUNCED,
    /** Unconfirmed rumour / speculative link. Never treat as fact in evaluation. */
    RUMOURED
}
