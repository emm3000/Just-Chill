package com.emm.domain.loan

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

internal fun ensurePersonProvided(personName: String) {
    if (personName.isBlank()) {
        throw DomainException.ValidationError("Person is required", ValidationCode.PersonRequired)
    }
}

internal fun ensureInterestInRange(interestBps: Int) {
    if (interestBps !in MIN_INTEREST_BPS..MAX_INTEREST_BPS) {
        throw DomainException.ValidationError(
            "Interest must be between $MIN_INTEREST_BPS and $MAX_INTEREST_BPS bps, got $interestBps",
            ValidationCode.InterestOutOfRange,
        )
    }
}
