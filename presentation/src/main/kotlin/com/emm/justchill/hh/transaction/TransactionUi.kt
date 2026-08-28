package com.emm.justchill.hh.transaction

import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import kotlinx.datetime.LocalDateTime

private const val UNKNOWN_CATEGORY = "Sin categoría"

data class TransactionUi(
    val transactionId: String,
    val type: TransactionType,
    val amount: String,
    val description: String,
    val occurredAt: LocalDateTime,
    val categoryName: String,
    val accountName: String,
    val category: CategoryUi,
) {

    /** What the user wrote; a blank one names the movement instead of announcing the blank. */
    val title: String get() = description.ifBlank { categoryName }

    /** Whatever [title] left unsaid — the category drops out once it is the title itself. */
    val subtitle: String
        get() = listOfNotNull(
            categoryName.takeIf { description.isNotBlank() },
            accountName.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
}

private fun TransactionWithCategory.toUi(): TransactionUi {
    val formattedNumber: String = fromCentsToSolesWith(amount)
    return TransactionUi(
        transactionId = transactionId.value,
        type = type,
        amount = when (type) {
            TransactionType.Income -> formatIncome(formattedNumber)
            TransactionType.Spend -> formatExpense(formattedNumber)
        },
        description = description,
        occurredAt = occurredAt,
        categoryName = category?.name ?: UNKNOWN_CATEGORY,
        accountName = accountName,
        category = CategoryUi(
            iconId = category?.icon,
            colorId = category?.color,
        ),
    )
}

fun List<TransactionWithCategory>.toUi(): List<TransactionUi> = map { it.toUi() }
