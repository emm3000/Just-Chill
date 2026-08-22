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

    // A digit string past Long's range parses to null; reading it as the largest amount there is
    // keeps the CTA off rather than handing centsToMoney a string it would throw on.
    private val amountCents: Long get() = amountDigits.toLongOrNull() ?: Long.MAX_VALUE

    // Warns early about the balance RegisterLoanPaymentUseCase enforces; the use case stays the
    // authority, this only stops the CTA inviting a round trip it knows ends in a rejection.
    val exceedsRemaining: Boolean get() = amountDigits.isSavableAmount() && amountCents > remainingCents

    val isSaveEnabled: Boolean get() = amountDigits.isSavableAmount() && !exceedsRemaining
}
