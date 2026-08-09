package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

/** Product rule. Public because the UI states the number in its own copy — one source of truth. */
const val MIN_SIGNUP_PASSWORD_LENGTH = 8

/**
 * Single validation guard for the auth domain.
 * Throws [error] immediately when [condition] is false.
 * Keeps the ThrowsCount to 1 per call site (detekt-compliant).
 */
internal fun ensure(condition: Boolean, error: DomainException) {
    if (!condition) throw error
}

/**
 * Validates an email address.
 *
 * Rules (minimally honest — server is the authority for full RFC compliance):
 * - Exactly one '@' character.
 * - Non-blank local part (before '@').
 * - Non-blank domain part (after '@').
 */
internal fun validateEmail(email: String) {
    val atCount = email.count { it == '@' }
    ensure(
        atCount == 1,
        DomainException.ValidationError("Email must contain exactly one '@'", ValidationCode.EmailInvalid),
    )
    val atIndex = email.indexOf('@')
    ensure(
        atIndex > 0,
        DomainException.ValidationError("Email local part must not be blank", ValidationCode.EmailInvalid),
    )
    ensure(
        atIndex < email.length - 1,
        DomainException.ValidationError("Email domain part must not be blank", ValidationCode.EmailInvalid),
    )
}

/**
 * Validates credentials for sign-in.
 *
 * - Email: validated via [validateEmail].
 * - Password: merely non-blank — existing accounts may have shorter passwords; the server is
 *   the authority on sign-in credential correctness.
 */
internal fun validateSignInCredentials(email: String, password: String) {
    validateEmail(email)
    ensure(
        password.isNotBlank(),
        DomainException.ValidationError("Password must not be blank", ValidationCode.PasswordRequired),
    )
}

/**
 * Validates credentials for sign-up.
 *
 * - Email: validated via [validateEmail].
 * - Password: minimum [MIN_SIGNUP_PASSWORD_LENGTH] characters (product rule; UI copy: "Mínimo 8 caracteres").
 */
internal fun validateSignUpCredentials(email: String, password: String) {
    validateEmail(email)
    ensure(
        password.isNotBlank(),
        DomainException.ValidationError("Password must not be blank", ValidationCode.PasswordRequired),
    )
    ensure(
        password.length >= MIN_SIGNUP_PASSWORD_LENGTH,
        DomainException.ValidationError(
            "Password must be at least $MIN_SIGNUP_PASSWORD_LENGTH characters",
            ValidationCode.PasswordTooShort,
        ),
    )
}
