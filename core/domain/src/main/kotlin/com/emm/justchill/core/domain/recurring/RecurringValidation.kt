package com.emm.justchill.core.domain.recurring

import com.emm.justchill.core.domain.shared.error.DomainException

internal fun ensure(condition: Boolean, error: DomainException) {
    if (!condition) throw error
}
