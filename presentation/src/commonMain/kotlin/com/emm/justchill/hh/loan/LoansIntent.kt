package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiIntent

sealed interface LoansIntent : UiIntent {
    data class OnPersonClick(val personKey: String) : LoansIntent
    data object OnAddLoanClick : LoansIntent
}
