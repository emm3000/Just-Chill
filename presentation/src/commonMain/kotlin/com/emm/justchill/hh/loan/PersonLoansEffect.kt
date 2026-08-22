package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiEffect

sealed interface PersonLoansEffect : UiEffect {
    data class NavigateToAddPayment(val loanId: String) : PersonLoansEffect
    data class NavigateToEditLoan(val loanId: String) : PersonLoansEffect
    data class ShowError(val message: String) : PersonLoansEffect
}
