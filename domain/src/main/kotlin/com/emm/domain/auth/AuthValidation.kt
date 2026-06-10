package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException

internal const val MIN_PASSWORD_LENGTH = 6

/**
 * Single validation guard for the auth domain.
 * Throws [error] immediately when [condition] is false.
 * Keeps the ThrowsCount to 1 per call site (detekt-compliant).
 */
internal fun ensure(condition: Boolean, error: DomainException) {
    if (!condition) throw error
}

internal fun validateCredentials(email: String, password: String) {
    ensure(email.isNotBlank(), DomainException.ValidationError("Email must not be blank"))
    ensure(email.contains('@'), DomainException.ValidationError("Email must contain '@'"))
    ensure(password.isNotBlank(), DomainException.ValidationError("Password must not be blank"))
    ensure(
        password.length >= MIN_PASSWORD_LENGTH,
        DomainException.ValidationError("Password must be at least 6 characters"),
    )
}
