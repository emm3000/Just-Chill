package com.emm.justchill.hh.recurring

import androidx.compose.runtime.Immutable
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.shared.Money
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.fromCentsToSolesWith

/**
 * UI representation of a pending recurring movement shown in the Home "Pendientes" section.
 *
 * [isVariableAmount] is true when [RecurringMovement.amount] is null — the ConfirmRecurringSheet
 * uses this flag to show an editable amount field and disable the confirm button until the user
 * supplies a valid amount.
 */
@Immutable
data class PendingRecurringUi(
    val templateId: String,
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

fun RecurringMovement.toPendingRecurringUi(): PendingRecurringUi {
    val isVariable = amount == null
    val formatted = if (isVariable) {
        "Variable"
    } else {
        val raw = fromCentsToSolesWith(amount as Money)
        when (type) {
            TransactionType.Income -> formatIncome(raw)
            TransactionType.Spend -> formatExpense(raw)
        }
    }
    return PendingRecurringUi(
        templateId = id.value,
        name = name,
        type = type,
        formattedAmount = formatted,
        isVariableAmount = isVariable,
        dayOfMonth = dayOfMonth,
        accountId = accountId.value,
        categoryId = categoryId?.value,
        description = description,
        fixedAmountCents = amount?.cents,
    )
}
