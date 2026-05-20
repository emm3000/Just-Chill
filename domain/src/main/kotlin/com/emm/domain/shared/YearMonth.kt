package com.emm.domain.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * A year + month, without day. Used to query and report on a calendar
 * month regardless of the user's clock or timezone.
 *
 * kotlinx-datetime doesn't ship a YearMonth type, so we roll our own.
 */
data class YearMonth(val year: Int, val month: Month) {

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
     * Inclusive start-of-month epoch millis in the given timezone.
     */
    fun startInclusiveMillis(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
        val firstDay = LocalDate(year, month, 1)
        return firstDay.atStartOfDayIn(timeZone).toEpochMilliseconds()
    }

    /**
     * Exclusive end-of-month epoch millis (= start of next month).
     */
    fun endExclusiveMillis(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long =
        next().startInclusiveMillis(timeZone)

    companion object {
        fun current(clock: Clock = Clock.System, timeZone: TimeZone = TimeZone.currentSystemDefault()): YearMonth {
            val today: LocalDateTime = clock.now().toLocalDateTime(timeZone)
            return YearMonth(year = today.year, month = today.month)
        }
    }
}
