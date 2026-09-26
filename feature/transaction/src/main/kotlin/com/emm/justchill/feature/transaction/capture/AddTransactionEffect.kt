package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface AddTransactionEffect : UiEffect {

    data object TransactionSaved : AddTransactionEffect

    data class ShowError(val message: String) : AddTransactionEffect
}
