package com.emm.justchill.hh.seetransactions

import com.emm.justchill.core.mvi.UiEffect

sealed interface SeeTransactionsEffect : UiEffect {
    data class ShowError(val message: String) : SeeTransactionsEffect
}
