package com.emm.domain.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A year + month, without day. Used to query and report on a calendar
 * month regardless of the user's clock or timezone.
 *
 * kotlinx-datetime doesn't ship a YearMonth type, so we roll our own.
 */
data class YearMonth(val year: Int, val month: Month) : Comparable<YearMonth> {

    override fun compareTo(other: YearMonth): Int = compareValuesBy(this, other, { it.year }, { it.month.ordinal })

    fun previous(): YearMonth {
        val prevOrdinal = month.ordinal - 1
        return if (prevOrdinal < 0) {
            YearMonth(year = year - 1, month = Month.DECEMBER)
        } else {
            YearMonth(year = year, month = Month.entries[prevOrdinal])
        }
    }

    fun next(): YearMonth {
        val nextOrdinal = month.ordinal + 1
        return if (nextOrdinal > Month.DECEMBER.ordinal) {
            YearMonth(year = year + 1, month = Month.JANUARY)
        } else {
            YearMonth(year = year, month = Month.entries[nextOrdinal])
        }
    }

    /**
     * Inclusive lower bound of the month as an ISO day string, `'2026-08-01'`.
     *
     * No timezone: the value it filters carries none either. The boundary between two months is a
     * calendar fact, and this is the calendar fact written down.
     */
    fun startInclusiveDay(): String = LocalDate(year, month, 1).toString()

    /** Exclusive upper bound of the month (= the first day of the next one). */
    fun endExclusiveDay(): String = next().startInclusiveDay()

    /**
     * Both bounds at once, for callers that query a window of months in one batch.
     */
    fun range(): MonthRange = MonthRange(
        startInclusive = startInclusiveDay(),
        endExclusive = endExclusiveDay(),
    )

    companion object {
        fun current(clock: Clock = Clock.System, timeZone: TimeZone = TimeZone.currentSystemDefault()): YearMonth {
            val today: LocalDateTime = clock.now().toLocalDateTime(timeZone)
            return YearMonth(year = today.year, month = today.month)
        }

        fun of(date: LocalDate): YearMonth = YearMonth(year = date.year, month = date.month)

        /**
         * The month an INSTANT falls in, as read in [timeZone]. The only callers are the ones that
         * genuinely hold an instant — `createdAt` and friends. A transaction's own occurrence is
         * not one of them; it already knows its calendar month without being asked where it is.
         */
        fun of(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): YearMonth =
            of(Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date)

        /**
         * [count] consecutive months ending at [endInclusive], oldest first — the order every
         * report window is read and drawn in.
         */
        fun windowEndingAt(endInclusive: YearMonth, count: Int): List<YearMonth> {
            val descending = ArrayList<YearMonth>(count.coerceAtLeast(0))
            var ym = endInclusive
            repeat(count) {
                descending.add(ym)
                ym = ym.previous()
            }
            return descending.asReversed()
        }
    }
}
