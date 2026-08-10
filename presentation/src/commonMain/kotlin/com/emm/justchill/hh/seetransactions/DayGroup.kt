package com.emm.justchill.hh.seetransactions

import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.shared.titlecaseFirstChar
import com.emm.justchill.hh.transaction.TransactionUi
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/**
 * One day's worth of rows, with [today] as the reference date the HOY/AYER branch is resolved
 * against. It is an input rather than an ambient `Clock.System.now()` read so that [primaryLabel]
 * is a pure function of the data class — the caller (the ViewModel) computes it once per mapping
 * pass from its injected clock, which also keeps every group in one pass on the same "today".
 */
data class DayGroup(val date: LocalDate, val today: LocalDate, val transactions: List<TransactionUi>) {

    /**
     * Prominent header label — the day itself, now that the month lives in the selector:
     * "HOY" / "AYER" for the two dates nobody needs a calendar for, otherwise weekday +
     * day number, e.g. "Martes 13".
     */
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
