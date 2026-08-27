package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiEffect

sealed interface PersonLoansEffect : UiEffect {
    data class NavigateToLoanDetail(val loanId: String) : PersonLoansEffect
}
