package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface AddTransactionEffect : UiEffect {

    data class TransactionSaved(val amount: Money) : AddTransactionEffect

    data class ShowError(val message: String) : AddTransactionEffect
}
