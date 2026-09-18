package com.emm.justchill.feature.loan

import com.emm.justchill.core.ui.mvi.UiEffect

sealed interface PersonLoansEffect : UiEffect {
    data class NavigateToLoanDetail(val loanId: String) : PersonLoansEffect
    data class ShowError(val message: String) : PersonLoansEffect
}
