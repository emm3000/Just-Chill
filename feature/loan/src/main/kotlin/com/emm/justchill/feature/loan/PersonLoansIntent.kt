package com.emm.justchill.feature.loan

import com.emm.justchill.core.ui.mvi.UiIntent

sealed interface PersonLoansIntent : UiIntent {
    data class OnLoanClick(val loanId: String) : PersonLoansIntent
}
