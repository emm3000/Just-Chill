package com.emm.justchill.core.time

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Pins the delay-until-midnight arithmetic in isolation, without a ViewModel around it.
 * `SeeTransactionsViewModelTest`/`SeeTransactionsPendingRecurringViewModelTest` cover how a ViewModel
 * reacts to a date change, using a fake [TodayFlow] backed by a `MutableStateFlow` instead of this
 * class — they never exercise the `delay` below.
 *
 * Every collector here runs on [kotlinx.coroutines.test.TestScope.backgroundScope] on purpose: this
 * flow's `delay` never completes on its own, so a foreground collector would make `advanceUntilIdle`
 * spin forever waiting for an event that is always about to be rescheduled (`MviViewModelTest.settle`
 * documents the same trap). `advanceTimeBy` still drains a background event inside its window; only
 * `advanceUntilIdle` treats background work as invisible.
 */
class ClockTodayFlowTest {

    private class MovingClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    @Test
    fun `emits today immediately on collection`() = runTest {
        val clock = MovingClock(Instant.parse("2026-08-15T12:00:00Z"))
        val todayFlow = ClockTodayFlow(clock, TimeZone.UTC)
        val dates = mutableListOf<LocalDate>()

        backgroundScope.launch { todayFlow().collect { dates += it } }
        runCurrent()

        assertEquals(listOf(LocalDate(2026, 8, 15)), dates)
    }

    @Test
    fun `re-emits exactly at the next local midnight, not before`() = runTest {
        val clock = MovingClock(Instant.parse("2026-08-15T12:00:00Z"))
        val todayFlow = ClockTodayFlow(clock, TimeZone.UTC)
        val dates = mutableListOf<LocalDate>()

        backgroundScope.launch { todayFlow().collect { dates += it } }
        runCurrent()
        assertEquals(listOf(LocalDate(2026, 8, 15)), dates)

        advanceTimeBy(12.hours - 1.seconds)
        assertEquals(listOf(LocalDate(2026, 8, 15)), dates, "midnight has not arrived yet")

        // Move the clock itself past local midnight too, so the loop's next iteration reads it —
        // the trap the ticket warns about: virtual time moving is not the clock moving.
        clock.instant = Instant.parse("2026-08-16T00:00:01Z")
        advanceTimeBy(2.seconds)

        assertEquals(listOf(LocalDate(2026, 8, 15), LocalDate(2026, 8, 16)), dates)
    }

    @Test
    fun `the emitted date is read in the injected zone, not the device default`() = runTest {
        // Both zones asserted from the SAME instant: on a machine whose own clock sits in the
        // asserted zone, checking only one zone would let the bug hide.
        val nearMidnight = MovingClock(Instant.parse("2026-09-01T02:00:00Z"))
        val lima = ClockTodayFlow(nearMidnight, UtcOffset(hours = -5).asTimeZone())
        val utc = ClockTodayFlow(nearMidnight, UtcOffset(hours = 0).asTimeZone())
        val limaDates = mutableListOf<LocalDate>()
        val utcDates = mutableListOf<LocalDate>()

        backgroundScope.launch { lima().collect { limaDates += it } }
        backgroundScope.launch { utc().collect { utcDates += it } }
        runCurrent()

        assertEquals(listOf(LocalDate(2026, 8, 31)), limaDates)
        assertEquals(listOf(LocalDate(2026, 9, 1)), utcDates)
    }

    @Test
    fun `a day whose local midnight is not 24h after the previous one still lands on the right date`() = runTest {
        // Spring-forward in Los Angeles: 2026-03-08 has only 23 hours, because 02:00 PST becomes
        // 03:00 PDT partway through it. A delay hard-coded to a fixed 24h would miss this rollover.
        val laZone = TimeZone.of("America/Los_Angeles")
        val clock = MovingClock(Instant.parse("2026-03-08T08:00:00Z")) // 2026-03-08T00:00 PST
        val todayFlow = ClockTodayFlow(clock, laZone)
        val dates = mutableListOf<LocalDate>()

        backgroundScope.launch { todayFlow().collect { dates += it } }
        runCurrent()
        assertEquals(listOf(LocalDate(2026, 3, 8)), dates)

        advanceTimeBy(23.hours - 1.seconds)
        assertEquals(listOf(LocalDate(2026, 3, 8)), dates, "23h have not yet passed in Los Angeles")

        clock.instant = Instant.parse("2026-03-09T07:00:01Z") // just past 2026-03-09T00:00 PDT
        advanceTimeBy(2.seconds)

        assertEquals(listOf(LocalDate(2026, 3, 8), LocalDate(2026, 3, 9)), dates)
    }
}
