package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException

internal const val MIN_SIGNUP_PASSWORD_LENGTH = 8

/**
 * Single validation guard for the auth domain.
 * Throws [error] immediately when [condition] is false.
 * Keeps the ThrowsCount to 1 per call site (detekt-compliant).
 */
internal fun ensure(condition: Boolean, error: DomainException) {
    if (!condition) throw error
}

/**
 * Validates credentials for sign-in.
 *
 * - Email: non-blank and contains '@'.
 * - Password: merely non-blank — existing accounts may have shorter passwords; the server is
 *   the authority on sign-in credential correctness.
 */
internal fun validateSignInCredentials(email: String, password: String) {
    ensure(email.isNotBlank(), DomainException.ValidationError("Email must not be blank"))
    ensure(email.contains('@'), DomainException.ValidationError("Email must contain '@'"))
    ensure(password.isNotBlank(), DomainException.ValidationError("Password must not be blank"))
}

/**
 * Validates credentials for sign-up.
 *
 * - Email: non-blank and contains '@'.
 * - Password: minimum [MIN_SIGNUP_PASSWORD_LENGTH] characters (product rule; UI copy: "Mínimo 8 caracteres").
 */
internal fun validateSignUpCredentials(email: String, password: String) {
    ensure(email.isNotBlank(), DomainException.ValidationError("Email must not be blank"))
    ensure(email.contains('@'), DomainException.ValidationError("Email must contain '@'"))
    ensure(password.isNotBlank(), DomainException.ValidationError("Password must not be blank"))
    ensure(
        password.length >= MIN_SIGNUP_PASSWORD_LENGTH,
        DomainException.ValidationError("Password must be at least $MIN_SIGNUP_PASSWORD_LENGTH characters"),
    )
}

/**
 * Validates an email address for operations that only require a valid email (e.g. resend confirmation).
 */
internal fun validateEmail(email: String) {
    ensure(email.isNotBlank(), DomainException.ValidationError("Email must not be blank"))
    ensure(email.contains('@'), DomainException.ValidationError("Email must contain '@'"))
}
