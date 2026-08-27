package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode

// Public because the UI states the number in its own copy — one source of truth.
const val MIN_SIGNUP_PASSWORD_LENGTH = 8

internal fun ensure(condition: Boolean, error: DomainException) {
    if (!condition) throw error
}

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

internal fun validateSignInCredentials(email: String, password: String) {
    validateEmail(email)
    ensure(
        password.isNotBlank(),
        DomainException.ValidationError("Password must not be blank", ValidationCode.PasswordRequired),
    )
}

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
