package com.emm.justchill.feature.auth

import com.emm.justchill.core.presentation.mvi.UiState

enum class AuthMode { SignIn, SignUp }

enum class Submitting { None, Email, Google }

sealed interface AuthUiState : UiState {

    data class Form(
        val email: String = "",
        val password: String = "",
        val mode: AuthMode = AuthMode.SignIn,
        val submitting: Submitting = Submitting.None,
    ) : AuthUiState {

        val heading: String
            get() = if (mode == AuthMode.SignIn) "Inicia sesión" else "Crea tu cuenta"

        val submitLabel: String
            get() = when (mode) {
                AuthMode.SignIn -> if (isSubmittingEmail) "Entrando…" else "Iniciar sesión"
                AuthMode.SignUp -> if (isSubmittingEmail) "Creando…" else "Crear cuenta"
            }

        val toggleLabel: String
            get() = if (mode == AuthMode.SignIn) "¿No tienes cuenta? Créala" else "¿Ya tienes cuenta? Inicia sesión"

        val isIdle: Boolean
            get() = submitting == Submitting.None

        val isSubmittingEmail: Boolean
            get() = submitting == Submitting.Email
    }

    data class CheckEmail(val email: String, val isResending: Boolean = false, val canResend: Boolean = true) :
        AuthUiState {

        val isResendEnabled: Boolean
            get() = !isResending && canResend

        val resendLabel: String
            get() = if (isResending) "Reenviando…" else "Reenviar enlace"
    }
}
