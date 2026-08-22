package com.emm.justchill.hh.loan

import com.emm.domain.loan.PaymentMethod
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.shared.relativeDayLabel
import com.emm.justchill.hh.transaction.isSavableAmount
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class LoanPaymentFormUi(
    val loanId: String,
    val today: LocalDate,
    val amountDigits: String = String.Empty,
    val method: PaymentMethod = PaymentMethod.Cash,
    val date: LocalDate? = null,
    val note: String = String.Empty,
    val isSaving: Boolean = false,
    val editingPaymentId: String? = null,
    // Captured once when the edit opens, so confirmPayment never has to fall back to "now" and
    // silently rewrite a real historical time. Null when creating.
    val originalPaidAt: LocalDateTime? = null,
    // Formatted in the ViewModel, where the Money values live — this is already the loan's
    // remaining balance with the edited payment's own old amount added back in, so it must never
    // be recomputed from a formatted string on the UI side.
    val maxAmountLabel: String? = null,
) {
    val dateLabel: String get() = relativeDayLabel(date ?: today, today)
    val isSaveEnabled: Boolean get() = amountDigits.isSavableAmount()
}
