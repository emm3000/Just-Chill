package com.emm.justchill.hh.account

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.emm.justchill.hh.shared.Empty

data class AddAccountUiState(
    val name: String = String.Empty,
    val balance: TextFieldValue = TextFieldValue("0.00", selection = TextRange("0.00".length)),
    val isEnabled: Boolean = false,
)