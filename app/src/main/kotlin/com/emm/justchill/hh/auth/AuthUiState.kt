package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiState

enum class AuthMode { SignIn, SignUp }

/** Tracks which submit path (if any) is currently in flight. */
enum class Submitting { None, Email, Google }

sealed interface AuthUiState : UiState {

    data class Form(
        val email: String = "",
        val password: String = "",
        val mode: AuthMode = AuthMode.SignIn,
        val submitting: Submitting = Submitting.None,
    ) : AuthUiState

    data class CheckEmail(val email: String, val isResending: Boolean = false, val canResend: Boolean = true) :
        AuthUiState
}
