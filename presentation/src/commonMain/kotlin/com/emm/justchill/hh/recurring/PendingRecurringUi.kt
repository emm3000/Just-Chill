package com.emm.justchill.hh.recurring

import com.emm.domain.recurring.PendingRecurring
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.periodKey
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.monthYearLabel

/**
 * UI representation of one pending recurring movement, for one period, in the Home "Pendientes"
 * section.
 *
 * [isVariableAmount] is true when [RecurringMovement.amount] is null — the ConfirmRecurringSheet
 * uses this flag to show an editable amount field and disable the confirm button until the user
 * supplies a valid amount.
 *
 * [id] is templateId + period, because one template can owe several months at once and the list
 * key has to stay unique. [isCatchUp] is true for anything older than the current month, which is
 * what the row uses to explain why an old month is showing up now.
 */
data class PendingRecurringUi(
    val id: String,
    val templateId: String,
    val period: YearMonth,
    val periodLabel: String,
    val isCatchUp: Boolean,
    val name: String,
    val type: TransactionType,
    val formattedAmount: String,
    val isVariableAmount: Boolean,
    val dayOfMonth: Int,
    val accountId: String,
    val categoryId: String?,
    val description: String,
    /** The raw fixed amount in cents, null if variable. Used to pre-fill ConfirmRecurringSheet. */
    val fixedAmountCents: Long?,
)

fun PendingRecurring.toPendingRecurringUi(currentMonth: YearMonth): PendingRecurringUi {
    val isVariable = movement.amount == null
    val formatted = if (isVariable) {
        "Variable"
    } else {
        val raw = fromCentsToSolesWith(movement.amount as Money)
        when (movement.type) {
            TransactionType.Income -> formatIncome(raw)
            TransactionType.Spend -> formatExpense(raw)
        }
    }
    return PendingRecurringUi(
        id = "${movement.id.value}@${periodKey(period)}",
        templateId = movement.id.value,
        period = period,
        periodLabel = period.monthYearLabel(),
        isCatchUp = period < currentMonth,
        name = movement.name,
        type = movement.type,
        formattedAmount = formatted,
        isVariableAmount = isVariable,
        dayOfMonth = movement.dayOfMonth,
        accountId = movement.accountId.value,
        categoryId = movement.categoryId?.value,
        description = movement.description,
        fixedAmountCents = movement.amount?.cents,
    )
}
