package com.emm.justchill.feature.loan

import com.emm.justchill.core.presentation.mvi.UiIntent

sealed interface LoansIntent : UiIntent {
    data class OnPersonClick(val personKey: String) : LoansIntent
    data object OnAddLoanClick : LoansIntent
}
