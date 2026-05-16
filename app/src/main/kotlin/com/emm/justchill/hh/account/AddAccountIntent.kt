package com.emm.justchill.hh.account

import com.emm.domain.account.AccountType
import com.emm.domain.account.Currency
import com.emm.justchill.core.mvi.UiIntent

sealed interface AddAccountIntent : UiIntent {

    data class OnNameChange(val value: String) : AddAccountIntent

    data class OnTypeChange(val value: AccountType) : AddAccountIntent

    data class OnCurrencyChange(val value: Currency) : AddAccountIntent

    data object OnSave : AddAccountIntent
}
