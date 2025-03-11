package com.emm.justchill.hh.account

import androidx.compose.ui.text.input.TextFieldValue
import com.emm.justchill.hh.shared.Empty

data class AccountUiState(
    val name: String = String.Empty,
    val description: String = String.Empty,
    val amount: TextFieldValue = TextFieldValue(""),
    val isEnabled: Boolean = false,
)