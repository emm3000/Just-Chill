package com.emm.justchill.hh.recurring

import com.emm.domain.recurring.PendingRecurring
import com.emm.domain.recurring.periodKey
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.monthYearLabel

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
