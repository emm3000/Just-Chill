package com.emm.justchill.hh.account.presentation

import androidx.compose.ui.text.input.TextFieldValue

sealed interface AccountAction {

    class OnNameChange(val value: String) : AccountAction

    class OnDescriptionChange(val value: String) : AccountAction

    class OnAmountChange(val value: TextFieldValue) : AccountAction

    data object OnSave : AccountAction
}