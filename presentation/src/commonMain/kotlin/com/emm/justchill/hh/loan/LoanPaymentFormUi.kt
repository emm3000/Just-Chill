package com.emm.justchill.hh.loan

import com.emm.domain.loan.PaymentMethod
import com.emm.domain.shared.error.ValidationCode
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.hh.shared.Empty
import com.emm.justchill.hh.shared.relativeDayLabel
import com.emm.justchill.hh.transaction.isSavableAmount
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class LoanPaymentFormUi(
    val loanId: String,
    val today: LocalDate,
    // The most this abono may be: what the loan still owes, with the edited abono's own old amount
    // added back in. Filled only by LoanDetailViewModel.withCap.
    val remainingCents: Long,
    val amountDigits: String = String.Empty,
    val method: PaymentMethod = PaymentMethod.Cash,
    val date: LocalDate? = null,
    val note: String = String.Empty,
    val isSaving: Boolean = false,
    val editingPaymentId: String? = null,
    // Captured once when the edit opens, so confirmPayment never has to fall back to "now" and
    // silently rewrite a real historical time. Null when creating.
    val originalPaidAt: LocalDateTime? = null,
    // remainingCents formatted, done where the Money values live so no caller has to do money
    // arithmetic on a display string. Null until the loan has loaded.
    val maxAmountLabel: String? = null,
) {
    val dateLabel: String get() = relativeDayLabel(date ?: today, today)

    val isSaveEnabled: Boolean get() = amountDigits.isSavableAmount() && !exceedsRemaining

    val amountError: String?
        get() = if (exceedsRemaining) ValidationCode.PaymentExceedsBalance.toUserMessage() else null

    // Warns early about the balance RegisterLoanPaymentUseCase enforces, so the CTA stops inviting
    // a round trip it knows ends in a rejection. The use case stays the authority.
    private val exceedsRemaining: Boolean get() = amountDigits.isSavableAmount() && amountCents > remainingCents

    private val amountCents: Long get() = amountDigits.toLongOrNull() ?: 0L
}
