package com.emm.justchill.core.ui.transaction

import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.TransactionWithCategory
import com.emm.justchill.core.ui.category.CategoryUi
import com.emm.justchill.core.ui.format.format
import com.emm.justchill.core.ui.format.formatExpense
import com.emm.justchill.core.ui.format.formatIncome
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

    val title: String get() = description.ifBlank { categoryName }

    val subtitle: String
        get() = listOfNotNull(
            categoryName.takeIf { description.isNotBlank() },
            accountName.takeIf { it.isNotBlank() },
        ).joinToString(" · ")
}

private fun TransactionWithCategory.toUi(): TransactionUi {
    val formattedNumber: String = amount.format()
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
