package com.emm.justchill.feature.recurring

import com.emm.justchill.core.domain.recurring.RecurringMovementDetails
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatExpense
import com.emm.justchill.core.ui.format.formatIncome

data class RecurringMovementUi(
    val id: String,
    val name: String,
    val type: TransactionType,
    val formattedAmount: String,
    val isVariableAmount: Boolean,
    val dayOfMonth: Int,
    val isActive: Boolean,
    val categoryName: String? = null,
    val categoryColor: String? = null,
    val accountName: String = "",
)

fun RecurringMovementDetails.toRecurringMovementUi(): RecurringMovementUi {
    val fixedAmount = amount
    val formatted = if (fixedAmount == null) {
        "Variable"
    } else {
        val raw = fixedAmount.format()
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
