package com.emm.domain.recurring

import com.emm.domain.shared.YearMonth
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number

private const val DAYS_IN_31_DAY_MONTH = 31
private const val DAYS_IN_30_DAY_MONTH = 30
private const val DAYS_IN_FEB_LEAP = 29
private const val DAYS_IN_FEB_NON_LEAP = 28
private const val LEAP_YEAR_DIVISOR = 4
private const val CENTURY_DIVISOR = 100
private const val QUAD_CENTURY_DIVISOR = 400

/**
 * Returns the number of days in the given [yearMonth].
 * Uses kotlinx-datetime only — no Android deps.
 */
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

/**
 * Clamps [dayOfMonth] to the last day of [yearMonth] when it exceeds the month length.
 */
fun effectiveDueDay(dayOfMonth: Int, yearMonth: YearMonth): Int = minOf(dayOfMonth, lengthOfMonth(yearMonth))

/**
 * Returns the period key for [yearMonth] in "YYYY-MM" format (e.g. "2026-02").
 */
fun periodKey(yearMonth: YearMonth): String {
    val mm = yearMonth.month.number.toString().padStart(2, '0')
    return "${yearMonth.year}-$mm"
}

/**
 * Returns true when [rm] is pending for [yearMonth] on [today].
 *
 * A template is pending when ALL three conditions hold:
 * 1. isActive = true
 * 2. lastConfirmedPeriod IS NULL OR != periodKey(yearMonth)
 * 3. today.dayOfMonth >= clamp(rm.dayOfMonth, yearMonth)
 *
 * The frequency field is explicitly consumed here so future non-monthly
 * values can be handled without silent fallthrough.
 */
fun isPending(rm: RecurringMovement, yearMonth: YearMonth, today: LocalDate): Boolean {
    val currentPeriod = periodKey(yearMonth)
    val dueDay = when (rm.frequency) {
        Frequency.Monthly -> effectiveDueDay(rm.dayOfMonth, yearMonth)
    }
    return rm.isActive && rm.lastConfirmedPeriod != currentPeriod && today.day >= dueDay
}
