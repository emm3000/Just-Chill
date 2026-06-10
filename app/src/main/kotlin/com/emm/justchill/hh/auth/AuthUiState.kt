package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiState

enum class AuthMode { SignIn, SignUp }

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val mode: AuthMode = AuthMode.SignIn,
    val isLoading: Boolean = false,
) : UiState
