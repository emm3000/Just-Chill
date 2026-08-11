package com.emm.domain.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The "last N days" window the frequent-category and frequent-combo suggestions are read over.
 *
 * It used to be `now - days * 24h`, which is a duration, not a number of days. Those two only agree
 * in a zone that never shifts its offset.
 */
class DateWindowTest {

    /** No DST — Peru has not observed it since 1994. The app's home zone. */
    private val lima = TimeZone.of("America/Lima")

    /** DST twice a year. Where the fixed-millis arithmetic came apart. */
    private val madrid = TimeZone.of("Europe/Madrid")

    private fun clockAt(date: LocalDate, hour: Int, minute: Int, zone: TimeZone): Clock = object : Clock {
        override fun now(): Instant = LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone)
    }

    @Test
    fun `the window starts at local midnight, whatever time of day it is asked`() {
        val today = LocalDate(2026, Month.AUGUST, 11)
        val expected = LocalDate(2026, Month.MAY, 13).atStartOfDayIn(lima).toEpochMilliseconds()

        // 90 days back from 11 August is 13 May, and the hour the question is asked cannot move it.
        assertEquals(expected, startOfDayDaysAgo(90, clockAt(today, 0, 1, lima), lima))
        assertEquals(expected, startOfDayDaysAgo(90, clockAt(today, 23, 59, lima), lima))
    }

    @Test
    fun `a window spanning a DST change counts calendar days, not fixed hours`() {
        // Madrid moved to CEST on 29 March 2026. A 90-day window ending 1 May crosses it, so the
        // day 90 days back is an hour longer ago than 90 * 24h — and the naive arithmetic lands an
        // hour into 31 January rather than at its start.
        val clock = clockAt(LocalDate(2026, Month.MAY, 1), hour = 12, minute = 0, zone = madrid)
        val expected = LocalDate(2026, Month.JANUARY, 31).atStartOfDayIn(madrid).toEpochMilliseconds()

        assertEquals(expected, startOfDayDaysAgo(90, clock, madrid))

        val naive = clock.now().toEpochMilliseconds() - 90L * 24 * 60 * 60 * 1000
        assertNotEquals(naive, startOfDayDaysAgo(90, clock, madrid))
    }

    @Test
    fun `a zone without DST agrees with the fixed-hours arithmetic at midnight`() {
        // Lima never shifts, so the two only ever differ by the time of day — the property that let
        // the old code look correct from Peru.
        val clock = clockAt(LocalDate(2026, Month.AUGUST, 11), hour = 0, minute = 0, zone = lima)
        val naive = clock.now().toEpochMilliseconds() - 90L * 24 * 60 * 60 * 1000

        assertEquals(naive, startOfDayDaysAgo(90, clock, lima))
    }

    @Test
    fun `a zero-day window starts at the beginning of today`() {
        val clock = clockAt(LocalDate(2026, Month.AUGUST, 11), hour = 16, minute = 30, zone = lima)
        val expected = LocalDate(2026, Month.AUGUST, 11).atStartOfDayIn(lima).toEpochMilliseconds()

        assertEquals(expected, startOfDayDaysAgo(0, clock, lima))
    }

    @Test
    fun `the window crosses a year boundary`() {
        val clock = clockAt(LocalDate(2026, Month.FEBRUARY, 10), hour = 9, minute = 0, zone = lima)
        val expected = LocalDate(2025, Month.NOVEMBER, 12).atStartOfDayIn(lima).toEpochMilliseconds()

        assertEquals(expected, startOfDayDaysAgo(90, clock, lima))
    }
}
