package com.emm.domain.shared

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
 *
 * The window is now an ISO day string, because the column it filters is one too: `occurredAt` is
 * zoneless local text, so the bound needs no zone either. The zone is still read for the one
 * question that cannot be answered without it — what today's local date is.
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
    fun `the window starts at the day itself, whatever time of day it is asked`() {
        val today = LocalDate(2026, Month.AUGUST, 11)

        // 90 days back from 11 August is 13 May, and the hour the question is asked cannot move it.
        assertEquals("2026-05-13", startOfDayDaysAgo(90, clockAt(today, 0, 1, lima), lima))
        assertEquals("2026-05-13", startOfDayDaysAgo(90, clockAt(today, 23, 59, lima), lima))
    }

    @Test
    fun `the bound is a bare day, so every hour of that first day is inside the window`() {
        val clock = clockAt(LocalDate(2026, Month.AUGUST, 11), hour = 12, minute = 0, zone = lima)
        val bound = startOfDayDaysAgo(90, clock, lima)

        // How SQL compares it: a bare 'YYYY-MM-DD' sorts before every wall-clock time on that day,
        // so `occurredAt >= bound` keeps the whole first day — including a movement at 00:00.
        assertEquals(true, "2026-05-13T00:00:00" >= bound)
        assertEquals(true, "2026-05-13T23:59:59" >= bound)
        assertEquals(false, "2026-05-12T23:59:59" >= bound)
    }

    @Test
    fun `a window spanning a DST change counts calendar days, not fixed hours`() {
        // Madrid moved to CEST on 29 March 2026, so a 90-day window ending 1 May crosses a day
        // that was only 23 hours long.
        val clock = clockAt(LocalDate(2026, Month.MAY, 1), hour = 12, minute = 0, zone = madrid)

        assertEquals("2026-01-31", startOfDayDaysAgo(90, clock, madrid))

        // What `now - 90 * 24h` actually lands on: 11:00 that day, not its start — an hour off
        // what the same arithmetic gives in a fixed-offset zone. Used as a lower bound it drops
        // that whole morning's movements, silently.
        val naive = Instant.fromEpochMilliseconds(
            clock.now().toEpochMilliseconds() - 90L * 24 * 60 * 60 * 1000,
        ).toLocalDateTime(madrid)
        assertEquals(LocalDate(2026, Month.JANUARY, 31), naive.date)
        assertNotEquals(LocalTime(0, 0), naive.time)
        assertNotEquals(LocalTime(12, 0), naive.time)
    }

    @Test
    fun `a zero-day window starts at today`() {
        val clock = clockAt(LocalDate(2026, Month.AUGUST, 11), hour = 16, minute = 30, zone = lima)

        assertEquals("2026-08-11", startOfDayDaysAgo(0, clock, lima))
    }

    @Test
    fun `the window crosses a year boundary`() {
        val clock = clockAt(LocalDate(2026, Month.FEBRUARY, 10), hour = 9, minute = 0, zone = lima)

        assertEquals("2025-11-12", startOfDayDaysAgo(90, clock, lima))
    }

    @Test
    fun `today is read in the given zone`() {
        // 22:00 on 11 August in Lima is already 12 August in Madrid, and the window moves with it.
        val instant = LocalDateTime(LocalDate(2026, Month.AUGUST, 11), LocalTime(22, 0)).toInstant(lima)
        val clock = object : Clock {
            override fun now(): Instant = instant
        }

        assertEquals("2026-08-11", startOfDayDaysAgo(0, clock, lima))
        assertEquals("2026-08-12", startOfDayDaysAgo(0, clock, madrid))
    }
}
