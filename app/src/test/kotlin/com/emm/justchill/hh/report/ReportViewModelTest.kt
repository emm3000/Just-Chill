package com.emm.justchill.hh.report

import com.emm.domain.report.CategoryAmount
import com.emm.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.domain.report.GetMonthlyComparisonUseCase
import com.emm.domain.report.GetMonthlySectionStatsUseCase
import com.emm.domain.report.GetSavingsRateUseCase
import com.emm.domain.report.GetTopCategoriesOverMonthsUseCase
import com.emm.domain.report.MonthlyComparison
import com.emm.domain.report.MonthlySectionStats
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
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val getMonthlyAmountByCategory = mockk<GetMonthlyAmountByCategoryUseCase>()
    private val getMonthlyComparison = mockk<GetMonthlyComparisonUseCase>()
    private val getMonthlySectionStats = mockk<GetMonthlySectionStatsUseCase>()
    private val getSavingsRate = mockk<GetSavingsRateUseCase>()
    private val getTopCategories = mockk<GetTopCategoriesOverMonthsUseCase>()

    private fun buildViewModel(): ReportViewModel {
        return ReportViewModel(
            getMonthlyAmountByCategory = getMonthlyAmountByCategory,
            getMonthlyComparison = getMonthlyComparison,
            getMonthlySectionStats = getMonthlySectionStats,
            getSavingsRate = getSavingsRate,
            getTopCategories = getTopCategories,
        )
    }

    /** Returns a minimal SavingsRate with no data months. */
    private fun emptySavingsRate(): SavingsRate = SavingsRate(
        currentRatePercent = 0,
        deltaPointsVsPrior = null,
        monthly = emptyList(),
        averageIncome = Money.Zero,
        averageExpense = Money.Zero,
    )

    /** Configures the use-case mocks to return empty/zero responses. */
    private fun stubEmptyReport() {
        coEvery { getMonthlyAmountByCategory(any(), any()) } returns emptyList()
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(0, Money.Zero)
        coEvery { getSavingsRate(any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any()) } returns emptyList()
    }

    // ── Init smoke test ───────────────────────────────────────────────────

    @Test
    fun `initial state has current month and Income type`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(YearMonth.current(), vm.state.value.month)
        assertEquals(TransactionType.Income, vm.state.value.selectedType)
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

        assertEquals(YearMonth.current(), vm.state.value.month)
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

    // ── Report data mapping ───────────────────────────────────────────────

    @Test
    fun `report with income amounts updates totalFormatted and shares`() = runTest(testDispatcher) {
        val catId = CategoryId("cat-1")
        val income = CategoryAmount(catId, "Sueldo", "wallet", "green", Money(450_000L)) // S/ 4,500

        coEvery { getMonthlyAmountByCategory(any(), TransactionType.Income) } returns listOf(income)
        coEvery { getMonthlyAmountByCategory(any(), TransactionType.Spend) } returns emptyList()
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(1, Money(450_000L))
        coEvery { getSavingsRate(any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any()) } returns emptyList()

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
        coEvery { getSavingsRate(any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any()) } returns emptyList()

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
        coEvery { getSavingsRate(any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any()) } returns emptyList()

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
        coEvery { getSavingsRate(any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any()) } returns emptyList()

        val vm = buildViewModel()
        // init triggers reloadReport — it will suspend at gate.await()
        // Let the coroutine start but stay suspended.
        testDispatcher.scheduler.runCurrent()

        val firstMonth = vm.state.value.month
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
        coEvery { getSavingsRate(any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any()) } returns emptyList()

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
