package com.emm.justchill.core.domain.auth

sealed interface SignUpResult {
    data class SignedIn(val user: AuthUser) : SignUpResult
    data object ConfirmationPending : SignUpResult
}
