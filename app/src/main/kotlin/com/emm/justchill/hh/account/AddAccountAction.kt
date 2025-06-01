package com.emm.justchill.hh.account

import androidx.compose.ui.text.input.TextFieldValue

sealed interface AddAccountAction {

    class OnNameChange(val value: String) : AddAccountAction

    class OnAmountChange(val value: TextFieldValue) : AddAccountAction

    data object OnSave : AddAccountAction
}