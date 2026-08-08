package com.kleos.transfers.domain;

/**
 * How precisely {@code dateOfBirth} is known.
 *
 * <p>{@link #YEAR} means only the birth year is known; the stored date uses
 * 1 July of that year as a mid-year anchor for age calculations. UI should
 * display the year only in that case.
 */
public enum DateOfBirthPrecision {
    DAY,
    YEAR
}
