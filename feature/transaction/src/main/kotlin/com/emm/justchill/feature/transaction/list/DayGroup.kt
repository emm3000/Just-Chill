package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.ui.format.SpanishDateFormat
import com.emm.justchill.core.ui.format.titlecaseFirstChar
import com.emm.justchill.core.ui.transaction.TransactionUi
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

data class DayGroup(val date: LocalDate, val today: LocalDate, val transactions: List<TransactionUi>) {

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
