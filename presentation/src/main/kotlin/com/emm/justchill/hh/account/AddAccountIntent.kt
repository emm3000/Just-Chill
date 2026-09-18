package com.emm.justchill.hh.account

import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.ui.mvi.UiIntent

sealed interface AddAccountIntent : UiIntent {

    data class OnNameChange(val value: String) : AddAccountIntent

    data class OnTypeChange(val value: AccountType) : AddAccountIntent

    data object OnSave : AddAccountIntent
}
