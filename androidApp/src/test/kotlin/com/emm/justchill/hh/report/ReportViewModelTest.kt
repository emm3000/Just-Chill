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
import com.emm.justchill.core.time.FakeTodayFlow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mid-May-2026, so `YearMonth.of(FIXED_DATE)` is May 2026 regardless of where a month boundary falls. */
private val FIXED_DATE = LocalDate(2026, Month.MAY, 15)

class ReportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val getMonthlyAmountByCategory = mockk<GetMonthlyAmountByCategoryUseCase>()
    private val getMonthlyComparison = mockk<GetMonthlyComparisonUseCase>()
    private val getMonthlySectionStats = mockk<GetMonthlySectionStatsUseCase>()
    private val getSavingsRate = mockk<GetSavingsRateUseCase>()
    private val getTopCategories = mockk<GetTopCategoriesOverMonthsUseCase>()

    private val currentMonth = YearMonth(2026, Month.MAY)

    private fun buildViewModel(dates: MutableStateFlow<LocalDate> = MutableStateFlow(FIXED_DATE)): ReportViewModel =
        ReportViewModel(
            getMonthlyAmountByCategory = getMonthlyAmountByCategory,
            getMonthlyComparison = getMonthlyComparison,
            getMonthlySectionStats = getMonthlySectionStats,
            getSavingsRate = getSavingsRate,
            getTopCategories = getTopCategories,
            todayFlow = FakeTodayFlow(dates),
        )

    private fun emptySavingsRate(): SavingsRate = SavingsRate(
        currentRatePercent = 0,
        deltaPointsVsPrior = null,
        monthly = emptyList(),
        averageIncome = Money.Zero,
        averageExpense = Money.Zero,
        monthsWithData = 0,
    )

    private fun stubEmptyReport() {
        coEvery { getMonthlyAmountByCategory(any(), any()) } returns emptyList()
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(0, Money.Zero)
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any()) } returns emptyList()
    }

    @Test
    fun `initial state has current month and Spend type`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(currentMonth, vm.state.value.month)
        assertEquals(TransactionType.Spend, vm.state.value.selectedType)
    }

    @Test
    fun `the month it opens on is YearMonth of the date TodayFlow reports`() = runTest(testDispatcher) {
        stubEmptyReport()
        val date = LocalDate(2026, Month.SEPTEMBER, 1)

        val vm = buildViewModel(MutableStateFlow(date))
        advanceUntilIdle()

        assertEquals(YearMonth.of(date), vm.state.value.month)
    }

    @Test
    fun `the trends window is asked for the month TodayFlow reports`() = runTest(testDispatcher) {
        stubEmptyReport()
        val date = LocalDate(2026, Month.SEPTEMBER, 1)
        val expectedMonth = YearMonth.of(date)

        buildViewModel(MutableStateFlow(date))
        advanceUntilIdle()

        coVerify { getSavingsRate(expectedMonth, any()) }
        coVerify { getTopCategories(any(), expectedMonth, any(), any()) }
    }

    @Test
    fun `state says whether the shown month is the current one`() = runTest(testDispatcher) {
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

        vm.onIntent(ReportIntent.SelectMonth(YearMonth(2024, Month.MARCH)))
        advanceUntilIdle()
        assertFalse(vm.state.value.isCurrentMonth)

        vm.onIntent(ReportIntent.SelectMonth(currentMonth))
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentMonth, "Picking the current month is not a special case")
    }

    @Test
    fun `a month rollover corrects isCurrentMonth with no month move`() = runTest(testDispatcher) {
        stubEmptyReport()
        val dates = MutableStateFlow(LocalDate(2026, Month.AUGUST, 31))

        val vm = buildViewModel(dates)
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentMonth, "August IS the current month on 31 August")

        dates.value = LocalDate(2026, Month.SEPTEMBER, 1)
        advanceUntilIdle()

        assertFalse(vm.state.value.isCurrentMonth, "the rollover must correct isCurrentMonth with no interaction")
        assertEquals(YearMonth(2026, Month.AUGUST), vm.state.value.month, "the month must not move")
    }

    @Test
    fun `a month rollover moves the trends bar marker, not just the pill`() = runTest(testDispatcher) {
        val august = YearMonth(2026, Month.AUGUST)
        val september = YearMonth(2026, Month.SEPTEMBER)
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(
            monthly = listOf(monthlyTotal(august), monthlyTotal(september)),
        )
        val dates = MutableStateFlow(LocalDate(2026, Month.AUGUST, 31))

        val vm = buildViewModel(dates)
        advanceUntilIdle()
        assertEquals(
            listOf(true, false),
            vm.state.value.trends.monthlyBars.map { it.isCurrentMonth },
            "on 31 August the August bar is the marked one",
        )

        dates.value = LocalDate(2026, Month.SEPTEMBER, 1)
        advanceUntilIdle()

        assertEquals(
            listOf(false, true),
            vm.state.value.trends.monthlyBars.map { it.isCurrentMonth },
            "the rollover must re-read the trends window with no interaction, or the chart keeps bolding August",
        )
    }

    private fun monthlyTotal(month: YearMonth) =
        MonthlyTotal(yearMonth = month, income = Money.Zero, expense = Money.Zero)

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

    @Test
    fun `SelectType changes selectedType in state`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(ReportIntent.SelectType(TransactionType.Income))
        advanceUntilIdle()

        assertEquals(TransactionType.Income, vm.state.value.selectedType)
    }

    @Test
    fun `SelectTab changes selectedTab in state`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(ReportIntent.SelectTab(ReportTab.Tendencias))
        advanceUntilIdle()

        assertEquals(ReportTab.Tendencias, vm.state.value.selectedTab)
    }

    @Test
    fun `deltaText carries no arrow glyph, the pill draws its own icon`() = runTest(testDispatcher) {
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(
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
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(
            currentRatePercent = 40,
            deltaPointsVsPrior = 7,
        )
        val vm = buildViewModel()
        advanceUntilIdle()

        val trends = vm.state.value.trends
        assertEquals("7 pts", trends.deltaText)
        assertEquals(true, trends.deltaIsPositive)
    }

    @Test
    fun `report with spend amounts updates totalFormatted and shares`() = runTest(testDispatcher) {
        val catId = CategoryId("cat-1")
        val spend = CategoryAmount(catId, "Comida", "wallet", "red", Money(450_000L))

        coEvery { getMonthlyAmountByCategory(any(), TransactionType.Income) } returns emptyList()
        coEvery { getMonthlyAmountByCategory(any(), TransactionType.Spend) } returns listOf(spend)
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(1, Money(450_000L))
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any()) } returns emptyList()

        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.totalFormatted.contains("4"), "Expected non-zero total, got: ${state.totalFormatted}")
        assertEquals(1, state.shares.size)
        assertEquals("Comida", state.shares.first().name)
        assertEquals(false, state.isEmpty)
        assertEquals(false, state.isMonthEmpty)
    }

    @Test
    fun `empty month sets isMonthEmpty to true`() = runTest(testDispatcher) {
        coEvery { getMonthlyAmountByCategory(any(), any()) } returns emptyList()
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(0, Money.Zero)
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any()) } returns emptyList()

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
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any()) } returns emptyList()

        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(25, state.comparisonPercent)
        assertTrue(state.comparisonAmountFormatted != null)
        assertTrue(state.comparisonText != null)
    }

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

    @Test
    fun `the reducer applies the new month before the in-flight load settles`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()

        coEvery { getMonthlyAmountByCategory(any(), any()) } coAnswers {
            gate.await()
            emptyList()
        }
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(0, Money.Zero)
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any()) } returns emptyList()

        val vm = buildViewModel()
        testDispatcher.scheduler.runCurrent()

        val secondMonth = YearMonth(2024, Month.JANUARY)

        vm.onIntent(ReportIntent.PreviousMonth)
        testDispatcher.scheduler.runCurrent()

        vm.onIntent(ReportIntent.SelectMonth(secondMonth))
        testDispatcher.scheduler.runCurrent()

        // The reducer runs synchronously, so state already reflects the second month before the gate opens.
        assertEquals(secondMonth, vm.state.value.month, "State must reflect the second request's month")

        gate.complete(Unit)
        advanceUntilIdle()

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

        assertTrue(
            invocationsBefore.any { it == month2 },
            "Expected month2 to be queried, got: $invocationsBefore",
        )
        assertEquals(month2, vm.state.value.month)
    }

    @Test
    fun `use case error emits ShowError effect`() = runTest(testDispatcher) {
        coEvery { getMonthlyAmountByCategory(any(), any()) } throws RuntimeException("db error")
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any()) } returns emptyList()

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
