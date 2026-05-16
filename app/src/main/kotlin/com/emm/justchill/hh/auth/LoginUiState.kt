package com.emm.justchill.hh.auth

import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty

data class LoginUiState(
    val email: String = String.Empty,
    val password: String = String.Empty,
    val isLoading: Boolean = false,
    val isValidFields: Boolean = false,
) : UiState
