package com.emm.justchill.feature.auth

import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.presentation.mvi.UiEffect

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
    ;

    val text: String
        get() = when (this) {
            GoogleAccountUnavailable -> "No encontramos una cuenta de Google en este teléfono."
            GoogleSignInFailed -> "No se pudo iniciar sesión con Google."
            ConfirmationLinkResent -> "Listo, te reenviamos el enlace."
        }
}
