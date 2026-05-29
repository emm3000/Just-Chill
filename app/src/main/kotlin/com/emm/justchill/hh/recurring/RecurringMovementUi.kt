package com.emm.justchill.hh.recurring

import androidx.compose.runtime.Immutable
import com.emm.domain.recurring.RecurringMovementDetails
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.fromCentsToSolesWith

/**
 * UI representation of a recurring movement template used in the management list (Slice 3).
 * Created here so Slice 2 mappers can reference it without creating a dependency cycle.
 */
@Immutable
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
    /** Token key (e.g. "green", "blue") — resolved to Color at render time via findById(key).primary */
    val categoryColor: String? = null,
    /** Account display name */
    val accountName: String = "",
)

/** Mapper for the list screen — sourced from the enriched details projection. */
fun RecurringMovementDetails.toRecurringMovementUi(): RecurringMovementUi {
    val isVariable = amount == null
    val formatted = when {
        isVariable -> "Variable"

        else -> {
            val raw = fromCentsToSolesWith(amount!!)
            when (type) {
                TransactionType.Income -> formatIncome(raw)
                TransactionType.Spend -> formatExpense(raw)
            }
        }
    }
    return RecurringMovementUi(
        id = id,
        name = name,
        type = type,
        formattedAmount = formatted,
        isVariableAmount = isVariable,
        dayOfMonth = dayOfMonth,
        isActive = isActive,
        categoryName = categoryName,
        categoryColor = categoryColor,
        accountName = accountName ?: "",
    )
}
