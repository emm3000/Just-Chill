package com.emm.justchill.hh.recurring

import com.emm.domain.recurring.RecurringMovementDetails
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class RecurringMovementUi(
    val id: String,
    val name: String,
    val type: TransactionType,
    val formattedAmount: String,
    val isVariableAmount: Boolean,
    val dayOfMonth: Int,
    val isActive: Boolean,
    /** null when category was deleted or never assigned */
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val accountName: String = "",
)

fun RecurringMovementDetails.toRecurringMovementUi(): RecurringMovementUi {
    val fixedAmount = amount
    val formatted = if (fixedAmount == null) {
        "Variable"
    } else {
        val raw = fromCentsToSolesWith(fixedAmount)
        when (type) {
            TransactionType.Income -> formatIncome(raw)
            TransactionType.Spend -> formatExpense(raw)
        }
    }
    return RecurringMovementUi(
        id = id,
        name = name,
        type = type,
        formattedAmount = formatted,
        isVariableAmount = fixedAmount == null,
        dayOfMonth = dayOfMonth,
        isActive = isActive,
        categoryName = categoryName,
        categoryColor = categoryColor,
        accountName = accountName.orEmpty(),
    )
}
