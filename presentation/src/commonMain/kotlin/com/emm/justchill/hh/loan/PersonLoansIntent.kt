package com.emm.justchill.hh.loan

import com.emm.domain.loan.PaymentMethod
import com.emm.justchill.core.mvi.UiIntent
import kotlinx.datetime.LocalDate

sealed interface PersonLoansIntent : UiIntent {
    data class OnAddPaymentClick(val loanId: String) : PersonLoansIntent
    data class OnEditLoanClick(val loanId: String) : PersonLoansIntent

    data class OnDeleteClick(val loanId: String) : PersonLoansIntent
    data object OnDeleteConfirm : PersonLoansIntent
    data object OnDeleteDismiss : PersonLoansIntent

    data class OnPaymentAmountChange(val digits: String) : PersonLoansIntent
    data class OnPaymentMethodChange(val method: PaymentMethod) : PersonLoansIntent
    data class OnPaymentDateSelected(val value: LocalDate) : PersonLoansIntent
    data class OnPaymentNoteChange(val value: String) : PersonLoansIntent
    data object OnPaymentConfirm : PersonLoansIntent
    data object OnPaymentDismiss : PersonLoansIntent
}
