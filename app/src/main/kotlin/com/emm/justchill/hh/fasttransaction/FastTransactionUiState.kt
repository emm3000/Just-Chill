package com.emm.justchill.hh.fasttransaction

import androidx.compose.ui.text.input.TextFieldValue
import com.emm.justchill.hh.shared.Empty

data class FastTransactionUiState(
    val amount: TextFieldValue = TextFieldValue("0.00"),
    val description: String = String.Empty,
    val isEnabled: Boolean = false,
)