package com.emm.domain.recurring

import com.emm.domain.shared.error.DomainException

/**
 * Single validation guard for the recurring domain.
 * Throws [error] immediately when [condition] is false.
 * Keeps the ThrowsCount to 1 per call site (detekt-compliant).
 */
internal fun ensure(condition: Boolean, error: DomainException) {
    if (!condition) throw error
}
