package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.TransactionWithCategory
import com.emm.justchill.core.ui.format.SpanishDateFormat
import com.emm.justchill.core.ui.format.titlecaseFirstChar
import com.emm.justchill.core.ui.transaction.TransactionUi
import com.emm.justchill.core.ui.transaction.toUi
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

data class DayGroup(
    val date: LocalDate,
    val today: LocalDate,
    val transactions: List<TransactionUi>,
    val spendTotal: Money? = null,
) {

    val primaryLabel: String
        get() = when (date) {
            today -> "HOY"

            today.minus(1, DateTimeUnit.DAY) -> "AYER"

            else -> {
                val weekday = SpanishDateFormat.fullWeekday(date.dayOfWeek.isoDayNumber)
                "${weekday.titlecaseFirstChar()} ${date.day}"
            }
        }

    // Only search results render this: their rows cross months, while in month mode the selector
    // already names the month once.
    val monthYearCaption: String
        get() = SpanishDateFormat.monthYear(date.year, date.month)
}

// The day a row belongs under is the day it carries. No zone, no conversion, nothing that can put
// the same transaction under a different header on a different device.
internal fun List<TransactionWithCategory>.toDayGroups(today: LocalDate): List<DayGroup> =
    groupBy { transaction -> transaction.occurredAt.date }
        .map { (date, transactions) ->
            DayGroup(
                date = date,
                today = today,
                transactions = transactions.toUi(),
                spendTotal = transactions.spendTotal(),
            )
        }

private fun List<TransactionWithCategory>.spendTotal(): Money? {
    val spendTransactions: List<TransactionWithCategory> = filter { it.type == TransactionType.Spend }
    if (spendTransactions.isEmpty()) return null
    return Money(spendTransactions.sumOf { it.amount.cents })
}
