package com.emm.domain.recurring

import com.emm.domain.shared.error.DomainException

internal fun ensure(condition: Boolean, error: DomainException) {
    if (!condition) throw error
}
