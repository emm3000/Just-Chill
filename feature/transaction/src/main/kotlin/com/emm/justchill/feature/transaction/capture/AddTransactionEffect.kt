package com.emm.justchill.feature.transaction.capture

import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface AddTransactionEffect : UiEffect {

    data class TransactionSaved(val month: YearMonth) : AddTransactionEffect

    data class ShowError(val message: String) : AddTransactionEffect
}
