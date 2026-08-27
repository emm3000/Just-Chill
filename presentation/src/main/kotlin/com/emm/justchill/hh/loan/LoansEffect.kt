package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiEffect

sealed interface LoansEffect : UiEffect {
    data class NavigateToPerson(val personKey: String) : LoansEffect
    data object NavigateToAddLoan : LoansEffect
    data class ShowError(val message: String) : LoansEffect
}
