package com.emm.domain.shared

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Builds a timestamp out of a calendar day taken from one value and a time of day taken from
 * another. Both ends are resolved in [zone]; reading the day in UTC instead would shift it by one
 * whenever the local time of day sits on the other side of UTC midnight.
 */
class DateAndTimeCombiner(private val zone: TimeZone = TimeZone.currentSystemDefault()) {

    /**
     * Day from [dateInMillis], time of day from the clock. For transactions being created, where
     * the picker only carries a day and "now" is the best guess at when it happened.
     */
    fun combineWithCurrentTime(dateInMillis: Long): Long =
        combine(dateInMillis, Clock.System.now().toLocalDateTime(zone).time)

    /**
     * Day from [dateInMillis], time of day from [timeSourceInMillis]. For transactions being
     * edited: moving one to another day must not restamp the hour it was recorded at. Passing the
     * same value twice returns it unchanged, so re-saving an untouched date is a no-op.
     */
    fun combineKeepingTimeOf(dateInMillis: Long, timeSourceInMillis: Long): Long =
        combine(dateInMillis, timeSourceInMillis.localDateTime().time)

    private fun combine(dateInMillis: Long, time: LocalTime): Long =
        LocalDateTime(dateInMillis.localDateTime().date, time).toInstant(zone).toEpochMilliseconds()

    private fun Long.localDateTime(): LocalDateTime = Instant.fromEpochMilliseconds(this).toLocalDateTime(zone)
}
