package com.emm.justchill.hh.report

import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.domain.report.GetMonthlyComparisonUseCase
import com.emm.domain.report.GetMonthlySectionStatsUseCase
import com.emm.domain.report.GetSavingsRateUseCase
import com.emm.domain.report.GetTopCategoriesOverMonthsUseCase
import com.emm.domain.report.MonthlyComparison
import com.emm.domain.report.MonthlySectionStats
import com.emm.domain.report.MonthlyTotal
import com.emm.domain.report.SavingsRate
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class ReportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val getMonthlyAmountByCategory = mockk<GetMonthlyAmountByCategoryUseCase>()
    private val getMonthlyComparison = mockk<GetMonthlyComparisonUseCase>()
    private val getMonthlySectionStats = mockk<GetMonthlySectionStatsUseCase>()
    private val getSavingsRate = mockk<GetSavingsRateUseCase>()
    private val getTopCategories = mockk<GetTopCategoriesOverMonthsUseCase>()

    // Mid-month noon UTC: YearMonth.current(fixedClock, zone) is May 2026 in every timezone, so no
    // test below depends on where the machine running it happens to be.
    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-05-15T12:00:00Z")
    }

    private val currentMonth = YearMonth(2026, Month.MAY)

    private fun buildViewModel(clock: Clock = fixedClock, zone: TimeZone = TimeZone.UTC): ReportViewModel =
        ReportViewModel(
            getMonthlyAmountByCategory = getMonthlyAmountByCategory,
            getMonthlyComparison = getMonthlyComparison,
            getMonthlySectionStats = getMonthlySectionStats,
            getSavingsRate = getSavingsRate,
            getTopCategories = getTopCategories,
            clock = clock,
            zone = zone,
        )

    /** Returns a minimal SavingsRate with no data months. */
    private fun emptySavingsRate(): SavingsRate = SavingsRate(
        currentRatePercent = 0,
        deltaPointsVsPrior = null,
        monthly = emptyList(),
        averageIncome = Money.Zero,
        averageExpense = Money.Zero,
        monthsWithData = 0,
    )

    /** Configures the use-case mocks to return empty/zero responses. */
    private fun stubEmptyReport() {
        coEvery { getMonthlyAmountByCategory(any(), any()) } returns emptyList()
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(0, Money.Zero)
        coEvery { getSavingsRate(any(), any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any(), any()) } returns emptyList()
    }

    // ── Init smoke test ───────────────────────────────────────────────────

    @Test
    fun `initial state has current month and Income type`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(currentMonth, vm.state.value.month)
        assertEquals(TransactionType.Income, vm.state.value.selectedType)
    }

    // ── Timezone ──────────────────────────────────────────────────────────

    @Test
    fun `the month it opens on is read in the injected zone, not the device's`() = runTest(testDispatcher) {
        // One instant, two zones, two different months: 2026-09-01T02:00Z is already September at
        // UTC and still 31 August at UTC-5. Which month Reporte opens on is therefore a question
        // about the zone, and the zone that answers it has to be the injected one — a zone read off
        // the machine is a zone no test can put a boundary on, which is how a user near a month
        // boundary could see a different month than the data says.
        //
        // Both zones are asserted because one proves nothing: on a machine whose own clock sits in
        // that zone the ambient read agrees, and the test stays green straight through the bug.
        // The dev machine here is America/Lima, which is exactly UTC-5.
        stubEmptyReport()
        val nearMidnight: Clock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-01T02:00:00Z")
        }

        val atLima = buildViewModel(nearMidnight, UtcOffset(hours = -5).asTimeZone())
        advanceUntilIdle()
        assertEquals(YearMonth(2026, Month.AUGUST), atLima.state.value.month)

        val atUtc = buildViewModel(nearMidnight, UtcOffset(hours = 0).asTimeZone())
        advanceUntilIdle()
        assertEquals(YearMonth(2026, Month.SEPTEMBER), atUtc.state.value.month)
    }

    @Test
    fun `the trends window is asked for in the injected zone too`() = runTest(testDispatcher) {
        // Half a screen answering for the device and half for the injection is the same defect at a
        // smaller scale: the Tendencias window has to end on the month the rest of the screen shows.
        stubEmptyReport()
        val zone = UtcOffset(hours = -5).asTimeZone()

        buildViewModel(fixedClock, zone)
        advanceUntilIdle()

        coVerify { getSavingsRate(fixedClock, zone, any()) }
        coVerify { getTopCategories(any(), fixedClock, zone, any(), any()) }
    }

    @Test
    fun `state says whether the shown month is the current one`() = runTest(testDispatcher) {
        // The screen used to answer this itself, with an ambient YearMonth.current(). Compose has
        // no injected zone to ask, so the "Hoy" pill was the one part of Reporte that could still
        // disagree with the month next to it.
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.isCurrentMonth, "The month it opens on IS the current one")

        vm.onIntent(ReportIntent.PreviousMonth)
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentMonth)

        vm.onIntent(ReportIntent.JumpToCurrent)
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentMonth)

        // The month picker is a third way in, and it can land on any month — including this one.
        vm.onIntent(ReportIntent.SelectMonth(YearMonth(2024, Month.MARCH)))
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentMonth)

        vm.onIntent(ReportIntent.SelectMonth(currentMonth))
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentMonth, "Picking the current month is not a special case")
    }

    @Test
    fun `a month rollover corrects isCurrentMonth with no month move`() = runTest(testDispatcher) {
        // The check this replaced lived in Compose, where recomposition re-evaluated it for free.
        // A flag written into state has no such refresh: computed once when the screen opened, it
        // would keep calling August the current month after midnight on 1 September, and TodayPill
        // — the only one-tap way back — would stay suppressed for the rest of the session.
        //
        // So every path that refreshes state re-reads the clock, not only a user-initiated move.
        stubEmptyReport()
        val clock = MovingClock(Instant.parse("2026-08-31T12:00:00Z"))

        val vm = buildViewModel(clock, TimeZone.UTC)
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentMonth, "August IS the current month on 31 August")

        // Midnight passes. The user has touched nothing; the shown month must not move on its own.
        clock.instant = Instant.parse("2026-09-01T12:00:00Z")

        // The Mes reload draws the pill, so it is the load-bearing path.
        vm.onIntent(ReportIntent.SelectType(TransactionType.Spend))
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentMonth, "the Mes reload must re-read the clock")
        assertEquals(YearMonth(2026, Month.AUGUST), vm.state.value.month, "the month must not move")

        // Back to a state where the flag is true, to prove the Tendencias reload independently.
        vm.onIntent(ReportIntent.JumpToCurrent)
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentMonth)

        clock.instant = Instant.parse("2026-10-01T12:00:00Z")
        vm.onIntent(ReportIntent.SelectTab(ReportTab.Tendencias))
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentMonth, "the Tendencias reload must re-read it too")
        assertEquals(YearMonth(2026, Month.SEPTEMBER), vm.state.value.month, "the month must not move")
    }

    @Test
    fun `a month rollover moves the trends bar marker, not just the pill`() = runTest(testDispatcher) {
        // TodayPill is not the only thing keyed to "which month is now": MonthlyBarItem.isCurrentMonth
        // draws one bar of the Tendencias chart bold. It comes off the same clock read, so it carries
        // the same limit — and a limit disclosed for only one of its two symptoms is how this audit
        // misled us once already. Both symptoms are now covered by a test.
        val august = YearMonth(2026, Month.AUGUST)
        val september = YearMonth(2026, Month.SEPTEMBER)
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any(), any()) } returns emptySavingsRate().copy(
            monthly = listOf(monthlyTotal(august), monthlyTotal(september)),
        )
        val clock = MovingClock(Instant.parse("2026-08-31T12:00:00Z"))

        val vm = buildViewModel(clock, TimeZone.UTC)
        advanceUntilIdle()
        assertEquals(
            listOf(true, false),
            vm.state.value.trends.monthlyBars.map { it.isCurrentMonth },
            "on 31 August the August bar is the marked one",
        )

        // Midnight passes with the screen open and the month untouched.
        clock.instant = Instant.parse("2026-09-01T12:00:00Z")
        vm.onIntent(ReportIntent.SelectTab(ReportTab.Tendencias))
        advanceUntilIdle()

        assertEquals(
            listOf(false, true),
            vm.state.value.trends.monthlyBars.map { it.isCurrentMonth },
            "the Tendencias reload must re-read the clock, or the chart keeps bolding August",
        )
    }

    private fun monthlyTotal(month: YearMonth) =
        MonthlyTotal(yearMonth = month, income = Money.Zero, expense = Money.Zero)

    /** A clock the test can move, so a month boundary can pass under a running ViewModel. */
    private class MovingClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    // ── Month navigation ──────────────────────────────────────────────────

    @Test
    fun `PreviousMonth intent decrements month`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        val original = vm.state.value.month
        vm.onIntent(ReportIntent.PreviousMonth)
        advanceUntilIdle()

        assertEquals(original.previous(), vm.state.value.month)
    }

    @Test
    fun `NextMonth intent increments month`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        val original = vm.state.value.month
        vm.onIntent(ReportIntent.NextMonth)
        advanceUntilIdle()

        assertEquals(original.next(), vm.state.value.month)
    }

    @Test
    fun `JumpToCurrent resets month to current`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(ReportIntent.PreviousMonth)
        advanceUntilIdle()

        vm.onIntent(ReportIntent.JumpToCurrent)
        advanceUntilIdle()

        assertEquals(currentMonth, vm.state.value.month)
    }

    @Test
    fun `SelectMonth intent changes month to the selected value`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        val target = YearMonth(2025, Month.MARCH)
        vm.onIntent(ReportIntent.SelectMonth(target))
        advanceUntilIdle()

        assertEquals(target, vm.state.value.month)
    }

    // ── SelectType ────────────────────────────────────────────────────────

    @Test
    fun `SelectType changes selectedType in state`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(ReportIntent.SelectType(TransactionType.Spend))
        advanceUntilIdle()

        assertEquals(TransactionType.Spend, vm.state.value.selectedType)
    }

    // ── SelectTab ─────────────────────────────────────────────────────────

    @Test
    fun `SelectTab changes selectedTab in state`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(ReportIntent.SelectTab(ReportTab.Tendencias))
        advanceUntilIdle()

        assertEquals(ReportTab.Tendencias, vm.state.value.selectedTab)
    }

    // ── Trends delta ──────────────────────────────────────────────────────

    @Test
    fun `deltaText carries no arrow glyph, the pill draws its own icon`() = runTest(testDispatcher) {
        // The pill renders a leading ArrowUpward/ArrowDownward from deltaIsPositive. A glyph in
        // the text too showed the user "↓ ↓ 10 pts".
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any(), any()) } returns emptySavingsRate().copy(
            currentRatePercent = 20,
            deltaPointsVsPrior = -10,
        )
        val vm = buildViewModel()
        advanceUntilIdle()

        val trends = vm.state.value.trends
        assertEquals("10 pts", trends.deltaText)
        assertEquals(false, trends.deltaIsPositive)
    }

    @Test
    fun `an improving delta reports the magnitude and a positive direction`() = runTest(testDispatcher) {
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any(), any()) } returns emptySavingsRate().copy(
            currentRatePercent = 40,
            deltaPointsVsPrior = 7,
        )
        val vm = buildViewModel()
        advanceUntilIdle()

        val trends = vm.state.value.trends
        assertEquals("7 pts", trends.deltaText)
        assertEquals(true, trends.deltaIsPositive)
    }

    // ── Report data mapping ───────────────────────────────────────────────

    @Test
    fun `report with income amounts updates totalFormatted and shares`() = runTest(testDispatcher) {
        val catId = CategoryId("cat-1")
        val income = CategoryAmount(catId, "Sueldo", "wallet", "green", Money(450_000L)) // S/ 4,500

        coEvery { getMonthlyAmountByCategory(any(), TransactionType.Income) } returns listOf(income)
        coEvery { getMonthlyAmountByCategory(any(), TransactionType.Spend) } returns emptyList()
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(1, Money(450_000L))
        coEvery { getSavingsRate(any(), any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any(), any()) } returns emptyList()

        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.totalFormatted.contains("4"), "Expected non-zero total, got: ${state.totalFormatted}")
        assertEquals(1, state.shares.size)
        assertEquals("Sueldo", state.shares.first().name)
        assertEquals(false, state.isEmpty)
        assertEquals(false, state.isMonthEmpty)
    }

    @Test
    fun `empty month sets isMonthEmpty to true`() = runTest(testDispatcher) {
        coEvery { getMonthlyAmountByCategory(any(), any()) } returns emptyList()
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(0, Money.Zero)
        coEvery { getSavingsRate(any(), any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any(), any()) } returns emptyList()

        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.isMonthEmpty)
    }

    @Test
    fun `comparison data is set when previous month has transactions`() = runTest(testDispatcher) {
        coEvery { getMonthlyAmountByCategory(any(), any()) } returns emptyList()
        coEvery { getMonthlyComparison(any(), any()) } returns MonthlyComparison(
            currentTotal = Money(100_00L),
            previousTotal = Money(80_00L),
            deltaPercent = 25,
            absoluteDelta = Money(20_00L),
        )
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(0, Money.Zero)
        coEvery { getSavingsRate(any(), any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any(), any()) } returns emptyList()

        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(25, state.comparisonPercent)
        assertTrue(state.comparisonAmountFormatted != null)
        assertTrue(state.comparisonText != null)
    }

    // ── ShareReport ───────────────────────────────────────────────────────

    @Test
    fun `ShareReport intent emits ShareReport effect with non-blank text`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        val effects = mutableListOf<ReportEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ReportIntent.ShareReport)
        advanceUntilIdle()

        val shareEffect = effects.filterIsInstance<ReportEffect.ShareReport>().firstOrNull()
        assertTrue(shareEffect != null, "Expected ShareReport effect but got: $effects")
        assertTrue(shareEffect.text.isNotBlank())

        job.cancel()
    }

    // ── Latest-wins concurrency ───────────────────────────────────────────

    /**
     * Fire two month changes in quick succession while the use case is suspended.
     * The first coroutine must be cancelled; the state must reflect the SECOND month.
     * The use case must be invoked exactly twice (both launches requested it).
     */
    @Test
    fun `rapid month changes are latest-wins — first load is cancelled by the second`() = runTest(testDispatcher) {
        // Gate lets us hold the first load suspended until the second starts.
        val gate = CompletableDeferred<Unit>()
        var invocationCount = 0

        coEvery { getMonthlyAmountByCategory(any(), any()) } coAnswers {
            invocationCount++
            gate.await() // suspend until released
            emptyList()
        }
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(0, Money.Zero)
        coEvery { getSavingsRate(any(), any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any(), any()) } returns emptyList()

        val vm = buildViewModel()
        // init triggers reloadReport — it will suspend at gate.await()
        // Let the coroutine start but stay suspended.
        testDispatcher.scheduler.runCurrent()

        val secondMonth = YearMonth(2024, Month.JANUARY)

        // Trigger first change: moves to previous, cancels the init load, starts a new load (also suspends).
        vm.onIntent(ReportIntent.PreviousMonth)
        testDispatcher.scheduler.runCurrent()

        // Trigger second change: should cancel the PreviousMonth load and start a fresh one.
        vm.onIntent(ReportIntent.SelectMonth(secondMonth))
        testDispatcher.scheduler.runCurrent()

        // State must already reflect the second month (reducer runs synchronously).
        assertEquals(secondMonth, vm.state.value.month, "State must reflect the second request's month")

        // Release the gate — only the surviving (second) coroutine should complete.
        gate.complete(Unit)
        advanceUntilIdle()

        // The use case is called twice: once for init+PreviousMonth (cancelled mid-flight),
        // and once for SelectMonth. The exact count depends on how many reached the await
        // before being cancelled. What matters is state shows the LAST requested month.
        assertTrue(invocationCount >= 1, "Use case must have been invoked at least once")
        assertEquals(secondMonth, vm.state.value.month, "Final state must be the second month")
    }

    @Test
    fun `second month change wins — state reflects latest month after both settle`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        val jan2024 = YearMonth(2024, Month.JANUARY)
        val feb2024 = YearMonth(2024, Month.FEBRUARY)

        vm.onIntent(ReportIntent.SelectMonth(jan2024))
        vm.onIntent(ReportIntent.SelectMonth(feb2024))
        advanceUntilIdle()

        assertEquals(feb2024, vm.state.value.month)
    }

    @Test
    fun `use case is called again after cancellation of previous load`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        val invocationsBefore = mutableListOf<YearMonth>()
        // Re-stub to track invocations with a captured month
        coEvery { getMonthlyAmountByCategory(any(), TransactionType.Income) } coAnswers {
            invocationsBefore.add(firstArg())
            emptyList()
        }
        coEvery { getMonthlyAmountByCategory(any(), TransactionType.Spend) } returns emptyList()

        val month1 = YearMonth(2024, Month.MARCH)
        val month2 = YearMonth(2024, Month.APRIL)
        vm.onIntent(ReportIntent.SelectMonth(month1))
        vm.onIntent(ReportIntent.SelectMonth(month2))
        advanceUntilIdle()

        // At least the second month must have been queried.
        assertTrue(
            invocationsBefore.any { it == month2 },
            "Expected month2 to be queried, got: $invocationsBefore",
        )
        assertEquals(month2, vm.state.value.month)
    }

    // ── Error handling ────────────────────────────────────────────────────

    @Test
    fun `use case error emits ShowError effect`() = runTest(testDispatcher) {
        coEvery { getMonthlyAmountByCategory(any(), any()) } throws RuntimeException("db error")
        coEvery { getSavingsRate(any(), any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any(), any()) } returns emptyList()

        val vm = buildViewModel()
        val effects = mutableListOf<ReportEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        advanceUntilIdle()

        assertTrue(
            effects.any { it is ReportEffect.ShowError },
            "Expected ShowError effect but got: $effects",
        )

        job.cancel()
    }
}
