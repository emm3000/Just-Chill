package com.emm.justchill.hh.auth

import com.emm.domain.shared.error.DomainException
import com.emm.justchill.core.mvi.UiEffect

sealed interface AuthEffect : UiEffect {
    data object NavigateBack : AuthEffect
    data object OpenEmailApp : AuthEffect
    data class ShowError(val error: DomainException) : AuthEffect
    data class Notify(val message: AuthMessage) : AuthEffect
}

enum class AuthMessage {
    GoogleAccountUnavailable,
    GoogleSignInFailed,
    ConfirmationLinkResent,
}
