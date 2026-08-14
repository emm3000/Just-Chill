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
private const val PERIOD_KEY_YEAR_LENGTH = 4
private const val MAX_PERIOD_KEY_YEAR = 9999

/**
 * How far back [pendingPeriods] will ask a user to catch up, current month included.
 *
 * Product limit. Past this, reconstructing back-payments from memory stops being realistic and the
 * pending list stops being a to-do list.
 */
const val MAX_CATCH_UP_MONTHS = 12

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
 *
 * Zero-padded on purpose: string ordering of these keys matches chronological ordering, which is
 * what lets the SQL guard in the data layer compare periods without parsing them.
 */
fun periodKey(yearMonth: YearMonth): String {
    val mm = yearMonth.month.number.toString().padStart(2, '0')
    return "${yearMonth.year}-$mm"
}

/**
 * Parses a "YYYY-MM" key back into a [YearMonth], or null when it is malformed.
 *
 * Rows arrive from other devices and from a backup file the user can hand-edit, so the stored key
 * is untrusted input like any other column.
 *
 * **The year is bounded to what [periodKey] can emit — four characters, 1..9999 — and that bound is
 * the whole point of this being stricter than "two numbers".** `ensureNotSettled` compares this
 * column as a plain string, so `"12345-07"` sorts above every real key: every confirmation is
 * refused as already-settled, while [pendingPeriods] reads it as a floor past the current month and
 * offers nothing to confirm. The template goes silent, and nothing in the app can put it back.
 *
 * `"9999-12"` sorts above every real key too and is accepted anyway, because the two are not the
 * same failure. A far-future *valid* mark means "settled through then" — someone wrote it, and
 * refusing confirmations under it is the monotonic guard working. A five-digit year is *malformed*:
 * no writer in this app can produce it, so it carries no such statement. Only the second is a
 * defect, and only the second is rejected — where the import turns it into null and keeps the row.
 *
 * The month stays lenient (`"2026-7"` reads as July) on purpose: the import repairs a near-miss by
 * parsing it and re-encoding through [periodKey], which it can only do while the parser reads it.
 */
fun parsePeriodKey(key: String): YearMonth? {
    val parts = key.split('-')
    if (parts.size != PERIOD_KEY_PARTS) return null
    val year = parts[0]
        .takeIf { it.length == PERIOD_KEY_YEAR_LENGTH }
        ?.toIntOrNull()
        ?.takeIf { it in 1..MAX_PERIOD_KEY_YEAR }
    val monthNumber = parts[1].toIntOrNull()?.takeIf { it in 1..Month.entries.size }
    return if (year != null && monthNumber != null) YearMonth(year, Month(monthNumber)) else null
}

/**
 * Every period [rm] still owes as of [today], oldest first.
 *
 * A recurring movement used to be pending only for the month the caller asked about, so a month
 * spent without opening the app was never surfaced again — the salary or the rent for it simply
 * never got recorded. The window now starts right after [RecurringMovement.lastConfirmedPeriod]
 * and runs to the current month.
 *
 * The window floor is the latest of three limits:
 * - the month after the high-water mark (everything up to it is settled),
 * - the month the template was created (it owes nothing from before it existed),
 * - [MAX_CATCH_UP_MONTHS] back from today.
 *
 * That last one is a product call, not a technical one: reconstructing more than a year of
 * back-payments from memory is not a thing anyone does, and an uncapped list would grow without
 * bound on a template left untouched for years.
 *
 * A past period is always due — its day has been and gone. Only the current month still has to
 * clear [effectiveDueDay]. The frequency is consumed explicitly so a future non-monthly value
 * cannot fall through silently.
 */
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
