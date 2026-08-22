package com.emm.justchill.hh.loan

import com.emm.justchill.core.mvi.UiIntent

sealed interface PersonLoansIntent : UiIntent {
    data class OnAddPaymentClick(val loanId: String) : PersonLoansIntent
    data class OnEditLoanClick(val loanId: String) : PersonLoansIntent

    data class OnDeleteClick(val loanId: String) : PersonLoansIntent
    data object OnDeleteConfirm : PersonLoansIntent
    data object OnDeleteDismiss : PersonLoansIntent
}
