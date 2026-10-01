package com.emm.justchill.feature.loan

import com.emm.justchill.core.domain.loan.PaymentMethod
import com.emm.justchill.core.domain.shared.error.ValidationCode
import com.emm.justchill.core.presentation.date.DateShortcut
import com.emm.justchill.core.presentation.date.dateShortcutsOf
import com.emm.justchill.core.presentation.error.toUserMessage
import com.emm.justchill.core.presentation.format.isSavableAmount
import com.emm.justchill.core.presentation.format.relativeDayLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class LoanPaymentFormUi(
    val loanId: String,
    val today: LocalDate,
    val remainingCents: Long,
    val amountDigits: String = "",
    val method: PaymentMethod = PaymentMethod.Cash,
    val date: LocalDate? = null,
    val note: String = "",
    val isSaving: Boolean = false,
    val editingPaymentId: String? = null,
    val originalPaidAt: LocalDateTime? = null,
    val maxAmountLabel: String? = null,
    val openSheet: PaymentSheet? = null,
) {
    val pickerDate: LocalDate get() = date ?: today

    val methodOptions: List<PaymentMethodOptionUi>
        get() = PaymentMethod.entries.map { PaymentMethodOptionUi(it, it.label, it == method) }

    val dateLabel: String get() = relativeDayLabel(pickerDate, today)

    val dateShortcuts: List<DateShortcut> get() = dateShortcutsOf(today)

    val isSaveEnabled: Boolean get() = amountDigits.isSavableAmount() && !exceedsRemaining

    val amountError: String?
        get() = if (exceedsRemaining) ValidationCode.PaymentExceedsBalance.toUserMessage() else null

    private val exceedsRemaining: Boolean get() = amountDigits.isSavableAmount() && amountCents > remainingCents

    private val amountCents: Long get() = amountDigits.toLongOrNull() ?: 0L
}
