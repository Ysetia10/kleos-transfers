package com.kleos.transfers.common.validation;

import java.util.UUID;

/**
 * Request shape exposing optional from/to club identifiers for transfer validation.
 */
public interface TransferClubPair {

    UUID fromClubId();

    UUID toClubId();
}
