package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.MonthRange
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetSavingsRateUseCaseTest {

    private val repository = mockk<TransactionStatsRepository>()
    private val useCase = GetSavingsRateUseCase(repository)

    private val currentMonth = YearMonth(2026, Month.MAY)

    private val amountsByMonth = mutableMapOf<YearMonth, Pair<Long, Long>>()

    @Before
    fun setUp() {
        coEvery { repository.monthlyAmountByCategoryForRanges(any()) } answers {
            firstArg<List<MonthRange>>().map { range ->
                val ym = YearMonth.of(LocalDate.parse(range.startInclusive))
                val (income, expense) = amountsByMonth[ym] ?: (0L to 0L)
                MonthCategoryAmounts(income = amounts(ym, income), expense = amounts(ym, expense))
            }
        }
    }

    private fun amounts(ym: YearMonth, cents: Long): List<CategoryAmount> = if (cents == 0L) {
        emptyList()
    } else {
        listOf(
            CategoryAmount(
                categoryId = CategoryId(ym.toString()),
                categoryName = "Cat $ym",
                categoryIcon = "icon",
                categoryColor = "blue",
                amount = Money(cents),
            ),
        )
    }

    private fun stubMonth(ym: YearMonth, type: TransactionType, amountCents: Long) {
        val (income, expense) = amountsByMonth[ym] ?: (0L to 0L)
        amountsByMonth[ym] = when (type) {
            TransactionType.Income -> amountCents to expense
            TransactionType.Spend -> income to amountCents
        }
    }

    private fun stubMonths(endInclusive: YearMonth, count: Int, income: Long, expense: Long): YearMonth {
        var ym = endInclusive
        repeat(count) {
            stubMonth(ym, TransactionType.Income, income)
            stubMonth(ym, TransactionType.Spend, expense)
            ym = ym.previous()
        }
        return ym
    }

    @Test
    fun `happy path - returns correct savings rate and delta`() = runTest {
        val priorEnd = stubMonths(YearMonth(2026, Month.MAY), count = 6, income = 600_000L, expense = 400_000L)
        stubMonths(priorEnd, count = 6, income = 500_000L, expense = 400_000L)

        val result = useCase(currentMonth, months = 6)

        assertEquals(33, result.currentRatePercent)
        assertEquals(13, result.deltaPointsVsPrior)
        assertEquals(6, result.monthly.size)
    }

    @Test
    fun `the whole window is read in a single round-trip, oldest month first`() = runTest {
        val ranges = slot<List<MonthRange>>()

        useCase(currentMonth, months = 6)

        coVerify(exactly = 1) { repository.monthlyAmountByCategoryForRanges(capture(ranges)) }
        assertEquals(12, ranges.captured.size)
        val months = ranges.captured.map { YearMonth.of(LocalDate.parse(it.startInclusive)) }
        assertEquals(YearMonth(2025, Month.JUNE), months.first())
        assertEquals(YearMonth(2026, Month.MAY), months.last())
        assertEquals(months.sorted(), months)
    }

    @Test
    fun `each range covers exactly its own calendar month`() = runTest {
        val ranges = slot<List<MonthRange>>()

        useCase(currentMonth, months = 6)

        coVerify { repository.monthlyAmountByCategoryForRanges(capture(ranges)) }
        ranges.captured.forEach { range ->
            val ym = YearMonth.of(LocalDate.parse(range.startInclusive))
            assertEquals(ym.startInclusiveDay(), range.startInclusive)
            assertEquals(ym.endExclusiveDay(), range.endExclusive)
        }
        ranges.captured.zipWithNext { earlier, later ->
            assertEquals(earlier.endExclusive, later.startInclusive)
        }
    }

    @Test
    fun `returns zero rate when income is zero`() = runTest {
        val result = useCase(currentMonth, months = 6)

        assertEquals(0, result.currentRatePercent)
        assertNull(result.deltaPointsVsPrior)
    }

    @Test
    fun `overspending reports a negative rate instead of hiding it as zero`() = runTest {
        stubMonths(YearMonth(2026, Month.MAY), count = 1, income = 100_000L, expense = 150_000L)

        val result = useCase(currentMonth, months = 6)

        assertEquals(-50, result.currentRatePercent)
    }

    @Test
    fun `rate reaches 100 when nothing is spent`() = runTest {
        stubMonths(YearMonth(2026, Month.MAY), count = 1, income = 100_000L, expense = 0L)

        val result = useCase(currentMonth, months = 6)

        assertEquals(100, result.currentRatePercent)
    }

    @Test
    fun `delta compares real rates, not clamped ones`() = runTest {
        val priorEnd = stubMonths(YearMonth(2026, Month.MAY), count = 6, income = 100_000L, expense = 150_000L)
        stubMonths(priorEnd, count = 6, income = 100_000L, expense = 250_000L)

        val result = useCase(currentMonth, months = 6)

        assertEquals(-50, result.currentRatePercent)
        assertEquals(100, result.deltaPointsVsPrior)
    }

    @Test
    fun `null delta when prior period has no income`() = runTest {
        stubMonths(YearMonth(2026, Month.MAY), count = 6, income = 600_000L, expense = 400_000L)

        val result = useCase(currentMonth, months = 6)

        assertNull(result.deltaPointsVsPrior)
    }

    @Test
    fun `monthly list has oldest month first`() = runTest {
        val result = useCase(currentMonth, months = 6)

        assertEquals(6, result.monthly.size)
        assertEquals(Month.DECEMBER, result.monthly.first().yearMonth.month)
        assertEquals(Month.MAY, result.monthly.last().yearMonth.month)
    }

    @Test
    fun `monthly totals land on the month they belong to`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        val april = may.previous()
        stubMonth(may, TransactionType.Income, 60_000L)
        stubMonth(april, TransactionType.Spend, 40_000L)

        val result = useCase(currentMonth, months = 6)

        val byMonth = result.monthly.associateBy { it.yearMonth }
        assertEquals(Money(60_000L), byMonth.getValue(may).income)
        assertEquals(Money.Zero, byMonth.getValue(may).expense)
        assertEquals(Money(40_000L), byMonth.getValue(april).expense)
        assertEquals(Money.Zero, byMonth.getValue(april).income)
    }

    @Test
    fun `averages divide by the whole window when every month has data`() = runTest {
        stubMonths(YearMonth(2026, Month.MAY), count = 6, income = 60_000L, expense = 40_000L)

        val result = useCase(currentMonth, months = 6)

        assertEquals(6, result.monthsWithData)
        assertEquals(Money(60_000L), result.averageIncome)
        assertEquals(Money(40_000L), result.averageExpense)
    }

    @Test
    fun `averages divide by the months that have data, not by the window size`() = runTest {
        stubMonths(YearMonth(2026, Month.MAY), count = 1, income = 60_000L, expense = 40_000L)

        val result = useCase(currentMonth, months = 6)

        assertEquals(1, result.monthsWithData)
        assertEquals(Money(60_000L), result.averageIncome)
        assertEquals(Money(40_000L), result.averageExpense)
    }

    @Test
    fun `a month with only expense still counts as a month with data`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        stubMonth(may, TransactionType.Spend, 40_000L)
        stubMonth(may.previous(), TransactionType.Income, 60_000L)

        val result = useCase(currentMonth, months = 6)

        assertEquals(2, result.monthsWithData)
        assertEquals(Money(30_000L), result.averageIncome)
        assertEquals(Money(20_000L), result.averageExpense)
    }

    @Test
    fun `averages are zero on an empty window instead of dividing by zero`() = runTest {
        val result = useCase(currentMonth, months = 6)

        assertEquals(0, result.monthsWithData)
        assertEquals(Money.Zero, result.averageIncome)
        assertEquals(Money.Zero, result.averageExpense)
    }
}
