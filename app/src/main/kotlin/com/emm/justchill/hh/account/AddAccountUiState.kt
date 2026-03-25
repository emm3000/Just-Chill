package com.emm.justchill.hh.account

import com.emm.justchill.hh.shared.Empty

data class AddAccountUiState(
    val name: String = String.Empty,
    val isEnabled: Boolean = false,
)
