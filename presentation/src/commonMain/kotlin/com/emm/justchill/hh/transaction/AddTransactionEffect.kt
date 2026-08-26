package com.emm.justchill.hh.transaction

import com.emm.justchill.core.mvi.UiEffect

sealed interface AddTransactionEffect : UiEffect {

    data object TransactionSaved : AddTransactionEffect

    data class ShowError(val message: String) : AddTransactionEffect
}
