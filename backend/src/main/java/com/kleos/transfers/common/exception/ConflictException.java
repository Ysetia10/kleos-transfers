package com.kleos.transfers.common.exception;

/**
 * Signals a business conflict that should surface as HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
