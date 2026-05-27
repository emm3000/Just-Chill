package com.emm.domain.recurring

import com.emm.domain.shared.YearMonth
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number

/**
 * Returns the number of days in the given [yearMonth].
 * Uses kotlinx-datetime only — no Android deps.
 */
fun lengthOfMonth(yearMonth: YearMonth): Int {
    val year = yearMonth.year
    val month = yearMonth.month
    return when (month) {
        Month.JANUARY, Month.MARCH, Month.MAY, Month.JULY,
        Month.AUGUST, Month.OCTOBER, Month.DECEMBER -> 31
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        Month.FEBRUARY -> if (isLeapYear(year)) 29 else 28
    }
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

/**
 * Clamps [dayOfMonth] to the last day of [yearMonth] when it exceeds the month length.
 */
fun effectiveDueDay(dayOfMonth: Int, yearMonth: YearMonth): Int =
    minOf(dayOfMonth, lengthOfMonth(yearMonth))

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
 */
fun isPending(rm: RecurringMovement, yearMonth: YearMonth, today: LocalDate): Boolean {
    if (!rm.isActive) return false
    val currentPeriod = periodKey(yearMonth)
    if (rm.lastConfirmedPeriod == currentPeriod) return false
    val dueDay = effectiveDueDay(rm.dayOfMonth, yearMonth)
    return today.day >= dueDay
}
