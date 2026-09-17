package com.emm.justchill.core.domain.shared

import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode

internal fun ensurePositiveAmount(amount: Money) {
    if (amount.cents <= 0) {
        throw DomainException.ValidationError("Amount must be greater than zero", ValidationCode.AmountMustBePositive)
    }
}
