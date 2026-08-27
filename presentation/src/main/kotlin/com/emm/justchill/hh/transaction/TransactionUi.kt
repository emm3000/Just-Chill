package com.emm.justchill.hh.transaction

import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.hh.shared.formatExpense
import com.emm.justchill.hh.shared.formatIncome
import com.emm.justchill.hh.shared.fromCentsToSolesWith
import com.emm.justchill.hh.shared.relativeDayLabel
import com.emm.justchill.hh.shared.timeLabel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class TransactionUi(
    val transactionId: String,
    val type: TransactionType,
    val amount: String,
    val description: String,
    val occurredAt: LocalDateTime,
    val readableDate: String,
    val readableTime: String,
    val category: CategoryUi,
)

private fun TransactionWithCategory.toUi(today: LocalDate): TransactionUi {
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
        readableDate = relativeDayLabel(occurredAt.date, today),
        readableTime = timeLabel(occurredAt.time),
        category = CategoryUi(
            iconId = category?.icon,
            colorId = category?.color,
        ),
    )
}

/**
 * [today] is threaded in from the caller's clock rather than read here, so every row in one mapping
 * pass resolves its Hoy/Ayer against the same day — and so a ViewModel's injected clock reaches the
 * label instead of being bypassed by an ambient one.
 */
fun List<TransactionWithCategory>.toUi(today: LocalDate) = map { it.toUi(today) }
