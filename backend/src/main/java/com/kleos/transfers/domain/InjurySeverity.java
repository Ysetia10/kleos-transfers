package com.kleos.transfers.domain;

/**
 * How disruptive an injury spell was to availability.
 *
 * <p>Bucketed rather than derived from days out so reported severity survives
 * incomplete or estimated return dates.
 */
public enum InjurySeverity {
    MINOR,
    MODERATE,
    SEVERE
}
