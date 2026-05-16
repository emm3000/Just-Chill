package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiState

data class SignUpUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val isValidFields: Boolean = false,
    val isChecked: Boolean = false,
    val isLoading: Boolean = false,
) : UiState
