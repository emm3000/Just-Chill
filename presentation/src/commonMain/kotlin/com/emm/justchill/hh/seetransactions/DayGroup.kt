package com.emm.justchill.hh.seetransactions

import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.shared.titlecaseFirstChar
import com.emm.justchill.hh.transaction.TransactionUi
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

    /**
     * Month context as a caption, e.g. "agosto 2026". Only search results render it — their
     * rows cross months; in month mode the selector already names the month once.
     */
    val monthYearCaption: String
        get() = SpanishDateFormat.monthYear(date.year, date.month)
}
