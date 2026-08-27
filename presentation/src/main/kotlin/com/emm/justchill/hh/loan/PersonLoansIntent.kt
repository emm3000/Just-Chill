package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiIntent

sealed interface PersonLoansIntent : UiIntent {
    data class OnLoanClick(val loanId: String) : PersonLoansIntent
}
