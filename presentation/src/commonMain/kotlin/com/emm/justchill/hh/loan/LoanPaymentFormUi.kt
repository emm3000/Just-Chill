package com.emm.justchill.hh.loan

import com.emm.domain.loan.PaymentMethod
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.shared.relativeDayLabel
import com.emm.justchill.hh.transaction.isSavableAmount
import kotlinx.datetime.LocalDate

data class LoanPaymentFormUi(
    val loanId: String,
    val today: LocalDate,
    val amountDigits: String = String.Empty,
    val method: PaymentMethod = PaymentMethod.Cash,
    val date: LocalDate? = null,
    val note: String = String.Empty,
    val isSaving: Boolean = false,
    val editingPaymentId: String? = null,
) {
    val dateLabel: String get() = relativeDayLabel(date ?: today, today)
    val isSaveEnabled: Boolean get() = amountDigits.isSavableAmount()
}
