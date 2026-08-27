package com.emm.domain.recurring

import com.emm.domain.shared.YearMonth
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number

private const val DAYS_IN_31_DAY_MONTH = 31
private const val DAYS_IN_30_DAY_MONTH = 30
private const val DAYS_IN_FEB_LEAP = 29
private const val DAYS_IN_FEB_NON_LEAP = 28
private const val LEAP_YEAR_DIVISOR = 4
private const val CENTURY_DIVISOR = 100
private const val QUAD_CENTURY_DIVISOR = 400
private const val PERIOD_KEY_PARTS = 2
private const val MIN_PERIOD_KEY_YEAR = 1000
private const val MAX_PERIOD_KEY_YEAR = 9999

// Product limit: past a year, reconstructing back-payments from memory stops being realistic and the
// pending list stops being a to-do list.
const val MAX_CATCH_UP_MONTHS = 12

fun lengthOfMonth(yearMonth: YearMonth): Int {
    val year = yearMonth.year
    val month = yearMonth.month
    return when (month) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER,
        -> DAYS_IN_31_DAY_MONTH

        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER,
        -> DAYS_IN_30_DAY_MONTH

        Month.FEBRUARY -> if (isLeapYear(year)) DAYS_IN_FEB_LEAP else DAYS_IN_FEB_NON_LEAP
    }
}

private fun isLeapYear(year: Int): Boolean =
    (year % LEAP_YEAR_DIVISOR == 0 && year % CENTURY_DIVISOR != 0) || (year % QUAD_CENTURY_DIVISOR == 0)

fun effectiveDueDay(dayOfMonth: Int, yearMonth: YearMonth): Int = minOf(dayOfMonth, lengthOfMonth(yearMonth))

fun periodKey(yearMonth: YearMonth): String {
    val mm = yearMonth.month.number.toString().padStart(2, '0')
    return "${yearMonth.year}-$mm"
}

// Paired with RecurringMovementLocalDataSource.ensureNotSettled in :data, which answers the same
// "is this mark settled" question by raw string comparison and bounds nothing. Widen this year range
// and the two disagree on which keys are well-formed, jamming the template with no tap left to fix it.
fun parsePeriodKey(key: String): YearMonth? {
    val parts = key.split('-')
    if (parts.size != PERIOD_KEY_PARTS) return null
    val year = parts[0].toIntOrNull()?.takeIf { it in MIN_PERIOD_KEY_YEAR..MAX_PERIOD_KEY_YEAR }
    val monthNumber = parts[1].toIntOrNull()?.takeIf { it in 1..Month.entries.size }
    return if (year != null && monthNumber != null) YearMonth(year, Month(monthNumber)) else null
}

fun pendingPeriods(rm: RecurringMovement, today: LocalDate, timeZone: TimeZone): List<YearMonth> {
    if (!rm.isActive) return emptyList()

    val currentMonth = YearMonth.of(today)
    val createdMonth = YearMonth.of(rm.createdAt, timeZone)
    val afterMark = rm.lastConfirmedPeriod?.let(::parsePeriodKey)?.next()
    var oldestAllowed = currentMonth
    repeat(MAX_CATCH_UP_MONTHS - 1) { oldestAllowed = oldestAllowed.previous() }

    val floor = listOfNotNull(createdMonth, oldestAllowed, afterMark).max()

    val periods = mutableListOf<YearMonth>()
    var period = floor
    while (period <= currentMonth) {
        val dueDay = when (rm.frequency) {
            Frequency.Monthly -> effectiveDueDay(rm.dayOfMonth, period)
        }
        if (period < currentMonth || today.day >= dueDay) periods.add(period)
        period = period.next()
    }
    return periods
}
