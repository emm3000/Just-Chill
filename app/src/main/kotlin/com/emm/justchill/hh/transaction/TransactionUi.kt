package com.emm.justchill.hh.transaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.ui.graphics.vector.ImageVector
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.CategoryColor
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.shared.fromCentsToSolesWith

data class TransactionUi(
    val transactionId: String,
    val type: TransactionType,
    val amount: String,
    val description: String,
    val date: Long,
    val readableDate: String,
    val readableTime: String,
    val category: CategoryUi,
)

data class CategoryUi(
    val categoryIcon: ImageVector,
    val categoryColor: CategoryColor,
)

private fun TransactionWithCategory.toUi(): TransactionUi {
    val formattedNumber: String = fromCentsToSolesWith(amount)
    return TransactionUi(
        transactionId = transactionId,
        type = type,
        amount = when (type) {
            TransactionType.Income -> "S/ $formattedNumber"
            TransactionType.Spend -> "S/ -$formattedNumber"
        },
        description = description,
        date = date,
        readableDate = DateUtils.millisToReadableFormat(date),
        readableTime = DateUtils.readableTime(date),
        category = CategoryUi(
            categoryIcon = category?.icon?.let(AppIconCatalog::findById)?.icon ?: Icons.Rounded.QuestionMark,
            categoryColor = category?.color?.let(::findById) ?: findById("gray"),
        ),
    )
}

fun List<TransactionWithCategory>.toUi() = map(TransactionWithCategory::toUi)