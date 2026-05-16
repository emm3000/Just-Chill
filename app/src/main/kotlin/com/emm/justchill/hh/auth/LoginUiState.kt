package com.emm.justchill.hh.auth

import androidx.compose.runtime.Immutable
import com.emm.justchill.core.mvi.UiState
import com.emm.justchill.hh.shared.Empty

@Immutable
data class LoginUiState(
    val email: String = String.Empty,
    val password: String = String.Empty,
    val isLoading: Boolean = false,
    val isValidFields: Boolean = false,
) : UiState
