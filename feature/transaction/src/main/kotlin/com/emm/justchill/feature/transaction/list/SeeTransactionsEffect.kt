package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface SeeTransactionsEffect : UiEffect {
    data class ShowError(val message: String) : SeeTransactionsEffect
}
