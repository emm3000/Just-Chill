package com.emm.justchill.hh.auth

import com.emm.justchill.hh.shared.Empty

data class LoginUiState(
    val email: String = String.Empty,
    val password: String = String.Empty,
    val isLoading: Boolean = false,
    val errorMsg: String? = null,
    val isValidFields: Boolean = false,
    val successLogin: Boolean = false,
)