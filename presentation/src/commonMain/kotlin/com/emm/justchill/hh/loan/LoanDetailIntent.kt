package com.emm.justchill.hh.loan

import com.emm.domain.loan.PaymentMethod
import com.emm.justchill.core.mvi.UiIntent
import kotlinx.datetime.LocalDate

sealed interface LoanDetailIntent : UiIntent {
    data object OnEditLoanClick : LoanDetailIntent

    data object OnDeleteLoanClick : LoanDetailIntent
    data object OnDeleteLoanConfirm : LoanDetailIntent
    data object OnDeleteLoanDismiss : LoanDetailIntent

    data class OnDeletePaymentClick(val paymentId: String) : LoanDetailIntent
    data object OnDeletePaymentConfirm : LoanDetailIntent
    data object OnDeletePaymentDismiss : LoanDetailIntent

    // Grouped so the ViewModel can dispatch all seven in one delegated `when` branch instead of
    // flattening every case into onIntent's own — the flat shape tripped CyclomaticComplexMethod.
    // Everything that owns state.payment lives here, including the click that opens it.
    sealed interface PaymentFormIntent : LoanDetailIntent {
        data object OnAddPaymentClick : PaymentFormIntent
        data class OnPaymentAmountChange(val digits: String) : PaymentFormIntent
        data class OnPaymentMethodChange(val method: PaymentMethod) : PaymentFormIntent
        data class OnPaymentDateSelected(val value: LocalDate) : PaymentFormIntent
        data class OnPaymentNoteChange(val value: String) : PaymentFormIntent
        data object OnPaymentConfirm : PaymentFormIntent
        data object OnPaymentDismiss : PaymentFormIntent
    }
}
