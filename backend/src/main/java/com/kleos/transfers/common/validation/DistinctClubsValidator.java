package com.kleos.transfers.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.UUID;

/**
 * Validates {@link TransferClubPair} requests.
 */
public class DistinctClubsValidator implements ConstraintValidator<DistinctClubs, TransferClubPair> {

    @Override
    public boolean isValid(TransferClubPair value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        UUID fromClubId = value.fromClubId();
        UUID toClubId = value.toClubId();
        if (fromClubId == null && toClubId == null) {
            return false;
        }
        return fromClubId == null || toClubId == null || !fromClubId.equals(toClubId);
    }
}
