package com.emm.justchill.hh.transaction

import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
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
        date = date,
        readableDate = DateUtils.friendlyDate(date),
        readableTime = DateUtils.readableTime(date),
        category = CategoryUi(
            iconId = category?.icon,
            colorId = category?.color,
        ),
    )
}

fun List<TransactionWithCategory>.toUi() = map(TransactionWithCategory::toUi)
