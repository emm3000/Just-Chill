package com.emm.justchill.feature.report

import com.emm.justchill.core.domain.report.CategoryAmount
import com.emm.justchill.core.domain.report.GetMonthlyAmountByCategoryUseCase
import com.emm.justchill.core.domain.report.GetMonthlyComparisonUseCase
import com.emm.justchill.core.domain.report.GetMonthlySectionStatsUseCase
import com.emm.justchill.core.domain.report.GetSavingsRateUseCase
import com.emm.justchill.core.domain.report.GetTopCategoriesOverMonthsUseCase
import com.emm.justchill.core.domain.report.MonthlyComparison
import com.emm.justchill.core.domain.report.MonthlySectionStats
import com.emm.justchill.core.domain.report.MonthlyTotal
import com.emm.justchill.core.domain.report.SavingsRate
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.presentation.format.CURRENCY_PREFIX
import com.emm.justchill.core.testing.FakeTodayFlow
import com.emm.justchill.core.testing.MainDispatcherRule
import com.emm.justchill.core.ui.atoms.PillTone
import com.emm.justchill.feature.report.components.comparisonPillTone
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val FIXED_DATE = LocalDate(2026, Month.MAY, 15)

class ReportViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val getMonthlyAmountByCategory: GetMonthlyAmountByCategoryUseCase = mockk()
    private val getMonthlyComparison: GetMonthlyComparisonUseCase = mockk()
    private val getMonthlySectionStats: GetMonthlySectionStatsUseCase = mockk()
    private val getSavingsRate: GetSavingsRateUseCase = mockk()
    private val getTopCategories: GetTopCategoriesOverMonthsUseCase = mockk()

    private val currentMonth: YearMonth = YearMonth.of(FIXED_DATE)

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
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(currentMonth, vm.state.value.month)
        assertEquals(TransactionType.Spend, vm.state.value.selectedType)
    }

    @Test
    fun `the month it opens on is YearMonth of the date TodayFlow reports`() = runTest(testDispatcher) {
        stubEmptyReport()
        val date = LocalDate(2026, Month.SEPTEMBER, 1)

        val vm: ReportViewModel = buildViewModel(MutableStateFlow(date))
        advanceUntilIdle()

        assertEquals(YearMonth.of(date), vm.state.value.month)
    }

    @Test
    fun `the trends window is asked for the month TodayFlow reports`() = runTest(testDispatcher) {
        stubEmptyReport()
        val date = LocalDate(2026, Month.SEPTEMBER, 1)
        val expectedMonth: YearMonth = YearMonth.of(date)

        buildViewModel(MutableStateFlow(date))
        advanceUntilIdle()

        coVerify { getSavingsRate(expectedMonth, any()) }
        coVerify { getTopCategories(any(), expectedMonth, any(), any()) }
    }

    @Test
    fun `state says whether the shown month is the current one`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
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
        val dates: MutableStateFlow<LocalDate> = MutableStateFlow(LocalDate(2026, Month.AUGUST, 31))

        val vm: ReportViewModel = buildViewModel(dates)
        advanceUntilIdle()
        assertTrue(vm.state.value.isCurrentMonth, "August IS the current month on 31 August")

        dates.value = LocalDate(2026, Month.SEPTEMBER, 1)
        advanceUntilIdle()

        assertFalse(vm.state.value.isCurrentMonth, "the rollover must correct isCurrentMonth with no interaction")
        assertEquals(YearMonth(2026, Month.AUGUST), vm.state.value.month, "the month must not move")

        vm.onIntent(ReportIntent.JumpToCurrent)
        advanceUntilIdle()
        assertEquals(
            YearMonth(2026, Month.SEPTEMBER),
            vm.state.value.month,
            "JumpToCurrent reads the rolled-over month",
        )
        assertTrue(vm.state.value.isCurrentMonth)

        dates.value = LocalDate(2026, Month.OCTOBER, 1)
        advanceUntilIdle()

        assertFalse(vm.state.value.isCurrentMonth)
        assertEquals(
            YearMonth(2026, Month.SEPTEMBER),
            vm.state.value.month,
            "a month reached by JumpToCurrent stays put too",
        )
    }

    @Test
    fun `a month rollover moves the trends bar marker, not just the pill`() = runTest(testDispatcher) {
        val august = YearMonth(2026, Month.AUGUST)
        val september = YearMonth(2026, Month.SEPTEMBER)
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(
            monthly = listOf(monthlyTotal(august), monthlyTotal(september)),
        )
        val dates: MutableStateFlow<LocalDate> = MutableStateFlow(LocalDate(2026, Month.AUGUST, 31))

        val vm: ReportViewModel = buildViewModel(dates)
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

    private fun monthlyTotal(month: YearMonth, income: Money = Money.Zero, expense: Money = Money.Zero) =
        MonthlyTotal(yearMonth = month, income = income, expense = expense)

    @Test
    fun `PreviousMonth intent decrements month`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val original: YearMonth = vm.state.value.month
        vm.onIntent(ReportIntent.PreviousMonth)
        advanceUntilIdle()

        assertEquals(original.previous(), vm.state.value.month)
    }

    @Test
    fun `NextMonth intent increments month`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val original: YearMonth = vm.state.value.month
        vm.onIntent(ReportIntent.NextMonth)
        advanceUntilIdle()

        assertEquals(original.next(), vm.state.value.month)
    }

    @Test
    fun `JumpToCurrent resets month to current`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
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
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val target = YearMonth(2025, Month.MARCH)
        vm.onIntent(ReportIntent.SelectMonth(target))
        advanceUntilIdle()

        assertEquals(target, vm.state.value.month)
    }

    @Test
    fun `SelectType changes selectedType in state`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(ReportIntent.SelectType(TransactionType.Income))
        advanceUntilIdle()

        assertEquals(TransactionType.Income, vm.state.value.selectedType)
    }

    @Test
    fun `SelectTab changes selectedTab in state`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(ReportIntent.SelectTab(ReportTab.Trends))
        advanceUntilIdle()

        assertEquals(ReportTab.Trends, vm.state.value.selectedTab)
    }

    @Test
    fun `deltaText carries no arrow glyph, the pill draws its own icon`() = runTest(testDispatcher) {
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(
            currentRatePercent = 20,
            deltaPointsVsPrior = -10,
        )
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val trends: TrendsUiData = vm.state.value.trends
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
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val trends: TrendsUiData = vm.state.value.trends
        assertEquals("7 pts", trends.deltaText)
        assertEquals(true, trends.deltaIsPositive)
    }

    @Test
    fun `an unchanged savings rate keeps the zero magnitude and reports no direction`() = runTest(testDispatcher) {
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(
            currentRatePercent = 30,
            deltaPointsVsPrior = 0,
        )
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val trends: TrendsUiData = vm.state.value.trends
        assertEquals("0 pts", trends.deltaText)
        assertNull(trends.deltaIsPositive)
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

        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val state: ReportUiState = vm.state.value
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

        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.isMonthEmpty)
    }

    private fun stubComparison(deltaPercent: Int, absoluteDelta: Money) {
        stubEmptyReport()
        coEvery { getMonthlyComparison(any(), any()) } returns MonthlyComparison(
            currentTotal = Money(100_00L),
            previousTotal = Money(80_00L),
            deltaPercent = deltaPercent,
            absoluteDelta = absoluteDelta,
        )
    }

    private fun TestScope.viewModelFor(
        type: TransactionType,
        deltaPercent: Int,
        absoluteDelta: Money = Money(if (deltaPercent < 0) -20_00L else 20_00L),
    ): ReportViewModel {
        stubComparison(deltaPercent, absoluteDelta)
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(ReportIntent.SelectType(type))
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `a spending rise stays monochrome, never danger`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Spend, deltaPercent = 25)

        assertEquals(true, vm.state.value.comparisonDirectionUp)
        assertEquals(false, vm.state.value.comparisonIsPositive)
        assertEquals(PillTone.Neutral, comparisonPillTone(vm.state.value.comparisonIsPositive))
    }

    @Test
    fun `a spending drop is favourable`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Spend, deltaPercent = -25)

        assertEquals(false, vm.state.value.comparisonDirectionUp)
        assertEquals(true, vm.state.value.comparisonIsPositive)
        assertEquals(PillTone.Pos, comparisonPillTone(vm.state.value.comparisonIsPositive))
    }

    @Test
    fun `an income drop stays monochrome, never danger`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Income, deltaPercent = -25)

        assertEquals(false, vm.state.value.comparisonDirectionUp)
        assertEquals(false, vm.state.value.comparisonIsPositive)
        assertEquals(PillTone.Neutral, comparisonPillTone(vm.state.value.comparisonIsPositive))
    }

    @Test
    fun `an income rise is favourable`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Income, deltaPercent = 25)

        assertEquals(true, vm.state.value.comparisonDirectionUp)
        assertEquals(true, vm.state.value.comparisonIsPositive)
        assertEquals(PillTone.Pos, comparisonPillTone(vm.state.value.comparisonIsPositive))
    }

    @Test
    fun `a spending drop reports the percent as a magnitude`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Spend, deltaPercent = -66)

        assertEquals(66, vm.state.value.comparisonPercent)
    }

    @Test
    fun `an income drop reports the percent as a magnitude`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Income, deltaPercent = -66)

        assertEquals(66, vm.state.value.comparisonPercent)
    }

    @Test
    fun `a rising spend pill reads its direction in words with the percent`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Spend, deltaPercent = 25)

        assertEquals("S/\u00A020 · 25%", vm.state.value.comparisonPillText)
        assertEquals("Subió S/\u00A020, 25%", vm.state.value.comparisonPillDescription)
    }

    @Test
    fun `a falling income pill reads its direction in words with the percent`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Income, deltaPercent = -66)

        assertEquals("S/\u00A020 · 66%", vm.state.value.comparisonPillText)
        assertEquals("Bajó S/\u00A020, 66%", vm.state.value.comparisonPillDescription)
    }

    @Test
    fun `a tiny-base percent caps in the pill text and its description`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Spend, deltaPercent = 44499)

        assertEquals("S/\u00A020 · más de 999%", vm.state.value.comparisonPillText)
        assertEquals("Subió S/\u00A020, más de 999%", vm.state.value.comparisonPillDescription)
    }

    @Test
    fun `a small fall that rounds to 0 percent reads Bajó`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Spend, deltaPercent = 0, absoluteDelta = Money(-5_00L))

        assertEquals(false, vm.state.value.comparisonDirectionUp)
        assertEquals(true, vm.state.value.comparisonIsPositive)
        assertEquals("Bajó ${CURRENCY_PREFIX}5, 0%", vm.state.value.comparisonPillDescription)
    }

    @Test
    fun `an unchanged month has no direction, no tint and reads Sin cambio`() = runTest(testDispatcher) {
        val vm: ReportViewModel = viewModelFor(TransactionType.Income, deltaPercent = 0, absoluteDelta = Money.Zero)

        assertEquals(null, vm.state.value.comparisonDirectionUp)
        assertEquals(null, vm.state.value.comparisonIsPositive)
        assertEquals(PillTone.Neutral, comparisonPillTone(vm.state.value.comparisonIsPositive))
        assertEquals("${CURRENCY_PREFIX}0 · 0%", vm.state.value.comparisonPillText)
        assertEquals("Sin cambio, 0%", vm.state.value.comparisonPillDescription)
    }

    @Test
    fun `a month with no previous month has no pill text`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(null, vm.state.value.comparisonPillText)
        assertEquals(null, vm.state.value.comparisonPillDescription)
    }

    @Test
    fun `a negative savings rate is a deficit and zero is not`() = runTest(testDispatcher) {
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(currentRatePercent = -12)
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.trends.isSavingsRateDeficit)

        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(currentRatePercent = 0)
        vm.onIntent(ReportIntent.SelectTab(ReportTab.Trends))
        advanceUntilIdle()
        assertFalse(vm.state.value.trends.isSavingsRateDeficit)
    }

    @Test
    fun `the trends bars scale against the tallest bar of the window`() = runTest(testDispatcher) {
        val august = YearMonth(2026, Month.AUGUST)
        val september = YearMonth(2026, Month.SEPTEMBER)
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(
            monthly = listOf(
                monthlyTotal(august, income = Money(400_00L), expense = Money(100_00L)),
                monthlyTotal(september, income = Money(200_00L), expense = Money.Zero),
            ),
        )
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val bars: List<MonthlyBarItem> = vm.state.value.trends.monthlyBars
        assertEquals(listOf(1f, 0.5f), bars.map { it.incomeFraction })
        assertEquals(listOf(0.25f, 0f), bars.map { it.expenseFraction })
    }

    @Test
    fun `a window with no amounts scales every bar to zero`() = runTest(testDispatcher) {
        stubEmptyReport()
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate().copy(
            monthly = listOf(monthlyTotal(currentMonth)),
        )
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val bar: MonthlyBarItem = vm.state.value.trends.monthlyBars.single()
        assertEquals(0f, bar.incomeFraction)
        assertEquals(0f, bar.expenseFraction)
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

        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val state: ReportUiState = vm.state.value
        assertEquals(25, state.comparisonPercent)
        assertTrue(state.comparisonAmountFormatted != null)
        assertTrue(state.comparisonText != null)
    }

    @Test
    fun `ShareReport intent emits ShareReport effect with non-blank text`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val effects: MutableList<ReportEffect> = mutableListOf()
        val job: Job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ReportIntent.ShareReport)
        advanceUntilIdle()

        val shareEffect: ReportEffect.ShareReport? = effects.filterIsInstance<ReportEffect.ShareReport>().firstOrNull()
        assertTrue(shareEffect != null, "Expected ShareReport effect but got: $effects")
        assertTrue(shareEffect.text.isNotBlank())

        job.cancel()
    }

    @Test
    fun `the reducer applies the new month before the in-flight load settles`() = runTest(testDispatcher) {
        val gate: CompletableDeferred<Unit> = CompletableDeferred()

        coEvery { getMonthlyAmountByCategory(any(), any()) } coAnswers {
            gate.await()
            emptyList()
        }
        coEvery { getMonthlyComparison(any(), any()) } returns null
        coEvery { getMonthlySectionStats(any(), any()) } returns MonthlySectionStats(0, Money.Zero)
        coEvery { getSavingsRate(any(), any()) } returns emptySavingsRate()
        coEvery { getTopCategories(any(), any(), any(), any()) } returns emptyList()

        val vm: ReportViewModel = buildViewModel()
        testDispatcher.scheduler.runCurrent()

        val secondMonth = YearMonth(2024, Month.JANUARY)

        vm.onIntent(ReportIntent.PreviousMonth)
        testDispatcher.scheduler.runCurrent()

        vm.onIntent(ReportIntent.SelectMonth(secondMonth))
        testDispatcher.scheduler.runCurrent()

        assertEquals(secondMonth, vm.state.value.month, "State must reflect the second request's month")

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(secondMonth, vm.state.value.month, "Final state must be the second month")
    }

    @Test
    fun `second month change wins — state reflects latest month after both settle`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
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
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()

        val invocationsBefore: MutableList<YearMonth> = mutableListOf()
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

        val vm: ReportViewModel = buildViewModel()
        val effects: MutableList<ReportEffect> = mutableListOf()
        val job: Job = launch { vm.effect.collect { effects.add(it) } }

        advanceUntilIdle()

        assertTrue(
            effects.any { it is ReportEffect.ShowError },
            "Expected ShowError effect but got: $effects",
        )

        job.cancel()
    }

    @Test
    fun `OnMonthSheetRequested opens the month sheet and OnMonthSheetDismissed closes it`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
        advanceUntilIdle()
        assertFalse(vm.state.value.showMonthSheet)

        vm.onIntent(ReportIntent.OnMonthSheetRequested)
        advanceUntilIdle()
        assertTrue(vm.state.value.showMonthSheet)

        vm.onIntent(ReportIntent.OnMonthSheetDismissed)
        advanceUntilIdle()
        assertFalse(vm.state.value.showMonthSheet)
    }

    @Test
    fun `a month pick leaves the month sheet open until the sheet dismisses itself`() = runTest(testDispatcher) {
        stubEmptyReport()
        val vm: ReportViewModel = buildViewModel()
        val states: MutableList<Boolean> = mutableListOf()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.map { it.showMonthSheet }.distinctUntilChanged().collect { states += it }
        }
        advanceUntilIdle()

        vm.onIntent(ReportIntent.OnMonthSheetRequested)
        vm.onIntent(ReportIntent.SelectMonth(YearMonth(2025, Month.MARCH)))
        advanceUntilIdle()

        assertEquals(listOf(false, true), states)

        vm.onIntent(ReportIntent.OnMonthSheetDismissed)
        advanceUntilIdle()

        assertEquals(listOf(false, true, false), states)
    }
}
