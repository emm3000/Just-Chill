package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiState

enum class AuthMode { SignIn, SignUp }

sealed interface AuthUiState : UiState {

    data class Form(
        val email: String = "",
        val password: String = "",
        val mode: AuthMode = AuthMode.SignIn,
        val isSubmitting: Boolean = false,
    ) : AuthUiState

    data class CheckEmail(
        val email: String,
        val isResending: Boolean = false,
    ) : AuthUiState
}
