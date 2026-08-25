package com.emm.justchill.core.time

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
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

    /**
     * Reads the virtual clock, so moving time moves the date too: a clock frozen while virtual time
     * advances lets a premature wake re-read the same date, which `distinctUntilChanged` then hides.
     * [slept] is the one thing virtual time cannot model — real time passing while `delay` is frozen,
     * which is what deep sleep does to Android's uptime-scheduled `delay`.
     */
    private class MovingClock(private val start: Instant, private val scheduler: TestCoroutineScheduler) : Clock {

        var slept: Duration = Duration.ZERO

        var reads: Int = 0
            private set

        override fun now(): Instant {
            reads++
            return start + scheduler.currentTime.milliseconds + slept
        }
    }

    @Test
    fun `emits today immediately on collection`() = runTest {
        val clock = MovingClock(Instant.parse("2026-08-15T12:00:00Z"), testScheduler)
        val todayFlow = ClockTodayFlow(clock, TimeZone.UTC, emptyFlow())
        val dates = mutableListOf<LocalDate>()

        backgroundScope.launch { todayFlow().collect { dates += it } }
        runCurrent()

        assertEquals(listOf(LocalDate(2026, 8, 15)), dates)
    }

    @Test
    fun `nothing wakes the flow between one day boundary and the next`() = runTest {
        val clock = MovingClock(Instant.parse("2026-08-15T12:00:00Z"), testScheduler)
        val todayFlow = ClockTodayFlow(clock, TimeZone.UTC, emptyFlow())

        backgroundScope.launch { todayFlow().collect { } }
        runCurrent()
        assertEquals(1, clock.reads, "one read, for the date it just emitted")

        advanceTimeBy(12.hours - 1.seconds)

        // Clock reads, not emissions: a poll re-reads the same date and distinctUntilChanged
        // swallows it, so the emitted list alone cannot tell a tick apart from a day boundary.
        assertEquals(1, clock.reads, "a day boundary is the event; nothing may wake this flow in between")
    }

    @Test
    fun `re-emits exactly at the next local midnight, not before`() = runTest {
        val clock = MovingClock(Instant.parse("2026-08-15T12:00:00Z"), testScheduler)
        val todayFlow = ClockTodayFlow(clock, TimeZone.UTC, emptyFlow())
        val dates = mutableListOf<LocalDate>()

        backgroundScope.launch { todayFlow().collect { dates += it } }
        runCurrent()
        assertEquals(listOf(LocalDate(2026, 8, 15)), dates)

        advanceTimeBy(12.hours - 1.seconds)
        assertEquals(listOf(LocalDate(2026, 8, 15)), dates, "midnight has not arrived yet")

        advanceTimeBy(2.seconds)

        assertEquals(listOf(LocalDate(2026, 8, 15), LocalDate(2026, 8, 16)), dates)
    }

    @Test
    fun `the emitted date is read in the injected zone, not the device default`() = runTest {
        // Both zones asserted from the SAME instant: on a machine whose own clock sits in the
        // asserted zone, checking only one zone would let the bug hide.
        val nearMidnight = MovingClock(Instant.parse("2026-09-01T02:00:00Z"), testScheduler)
        val lima = ClockTodayFlow(nearMidnight, UtcOffset(hours = -5).asTimeZone(), emptyFlow())
        val utc = ClockTodayFlow(nearMidnight, UtcOffset(hours = 0).asTimeZone(), emptyFlow())
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
        val clock = MovingClock(Instant.parse("2026-03-08T08:00:00Z"), testScheduler) // 2026-03-08T00:00 PST
        val todayFlow = ClockTodayFlow(clock, laZone, emptyFlow())
        val dates = mutableListOf<LocalDate>()

        backgroundScope.launch { todayFlow().collect { dates += it } }
        runCurrent()
        assertEquals(listOf(LocalDate(2026, 3, 8)), dates)

        advanceTimeBy(23.hours - 1.seconds)
        assertEquals(listOf(LocalDate(2026, 3, 8)), dates, "23h have not yet passed in Los Angeles")

        advanceTimeBy(2.seconds)

        assertEquals(listOf(LocalDate(2026, 3, 8), LocalDate(2026, 3, 9)), dates)
    }

    @Test
    fun `a foreground resume delivers the date the midnight wake has not fired for yet`() = runTest {
        // A device dozing across midnight: 13 real hours pass, but Android schedules `delay`
        // against uptime, which does not advance in deep sleep — so the midnight wake is still
        // pending. Returning to the app is what must show the user the right day.
        val resumes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val clock = MovingClock(Instant.parse("2026-08-15T12:00:00Z"), testScheduler)
        val todayFlow = ClockTodayFlow(clock, TimeZone.UTC, resumes)
        val dates = mutableListOf<LocalDate>()

        backgroundScope.launch { todayFlow().collect { dates += it } }
        runCurrent()
        assertEquals(listOf(LocalDate(2026, 8, 15)), dates)

        clock.slept = 13.hours
        resumes.emit(Unit)
        runCurrent()

        assertEquals(listOf(LocalDate(2026, 8, 15), LocalDate(2026, 8, 16)), dates)
    }
}
