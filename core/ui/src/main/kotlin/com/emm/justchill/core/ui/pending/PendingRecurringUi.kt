package com.emm.justchill.core.ui.pending

import com.emm.justchill.core.domain.recurring.PendingRecurring
import com.emm.justchill.core.domain.recurring.periodKey
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatExpense
import com.emm.justchill.core.ui.format.formatIncome
import com.emm.justchill.core.ui.format.monthYearLabel

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
    val isVariable: Boolean = movement.amount == null
    val formatted: String = if (isVariable) {
        "Variable"
    } else {
        val raw: String = (movement.amount as Money).format()
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
