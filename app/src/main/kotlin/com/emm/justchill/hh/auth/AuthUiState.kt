package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiState

enum class AuthMode { SignIn, SignUp }

enum class AuthStep { Form, CheckEmail }

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val mode: AuthMode = AuthMode.SignIn,
    val isLoading: Boolean = false,
    val step: AuthStep = AuthStep.Form,
    val confirmationEmail: String = "",
    val isResending: Boolean = false,
) : UiState
