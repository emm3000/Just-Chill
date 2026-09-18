package com.emm.justchill.hh.transaction

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface EditTransactionEffect : UiEffect {

    data object TransactionUpdated : EditTransactionEffect

    data object TransactionDeleted : EditTransactionEffect

    data class ShowError(val message: String) : EditTransactionEffect
}
