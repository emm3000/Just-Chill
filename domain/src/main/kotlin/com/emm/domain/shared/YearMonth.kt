package com.emm.domain.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

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

    fun startInclusiveDay(): String = LocalDate(year, month, 1).toString()

    fun endExclusiveDay(): String = next().startInclusiveDay()

    fun range(): MonthRange = MonthRange(
        startInclusive = startInclusiveDay(),
        endExclusive = endExclusiveDay(),
    )

    companion object {
        fun of(date: LocalDate): YearMonth = YearMonth(year = date.year, month = date.month)

        fun of(epochMillis: Long, timeZone: TimeZone): YearMonth =
            of(Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date)

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
