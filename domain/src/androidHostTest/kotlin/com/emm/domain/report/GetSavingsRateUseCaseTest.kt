package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class GetSavingsRateUseCaseTest {

    private val repository = mockk<TransactionStatsRepository>()
    private val useCase = GetSavingsRateUseCase(repository)

    // Fixed clock: May 2026
    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-05-15T12:00:00Z")
    }

    private fun cat(id: String, amountCents: Long) = CategoryAmount(
        categoryId = CategoryId(id),
        categoryName = "Cat $id",
        categoryIcon = "icon",
        categoryColor = "blue",
        amount = Money(amountCents),
    )

    private fun stubMonth(ym: YearMonth, type: TransactionType, amountCents: Long) {
        val items = if (amountCents == 0L) {
            emptyList()
        } else {
            listOf(cat(ym.toString(), amountCents))
        }
        coEvery {
            repository.monthlyAmountByCategory(type, ym.startInclusiveMillis(), ym.endExclusiveMillis())
        } returns items
    }

    private fun stubAllMonthsBlank(months: Int = 12) {
        var ym = YearMonth(2026, Month.MAY)
        repeat(months) {
            stubMonth(ym, TransactionType.Income, 0L)
            stubMonth(ym, TransactionType.Spend, 0L)
            ym = ym.previous()
        }
    }

    @Test
    fun `happy path - returns correct savings rate and delta`() = runTest {
        stubAllMonthsBlank()
        val currentEnd = YearMonth(2026, Month.MAY)
        var ym = currentEnd
        repeat(6) {
            stubMonth(ym, TransactionType.Income, 600_000L)
            stubMonth(ym, TransactionType.Spend, 400_000L)
            ym = ym.previous()
        }
        val priorEnd = run {
            var m = YearMonth(2026, Month.MAY)
            repeat(6) { m = m.previous() }
            m
        }
        var pm = priorEnd
        repeat(6) {
            stubMonth(pm, TransactionType.Income, 500_000L)
            stubMonth(pm, TransactionType.Spend, 400_000L)
            pm = pm.previous()
        }

        val result = useCase(months = 6, clock = fixedClock)

        assertEquals(33, result.currentRatePercent)
        // prior rate = (5000-4000)/5000 * 100 = 20, delta = 33 - 20 = 13
        assertEquals(13, result.deltaPointsVsPrior)
        assertEquals(6, result.monthly.size)
    }

    @Test
    fun `returns zero rate when income is zero`() = runTest {
        stubAllMonthsBlank(12)

        val result = useCase(months = 6, clock = fixedClock)

        assertEquals(0, result.currentRatePercent)
        assertNull(result.deltaPointsVsPrior)
    }

    @Test
    fun `rate is clamped to 0 when expenses exceed income`() = runTest {
        stubAllMonthsBlank()
        val ym = YearMonth(2026, Month.MAY)
        stubMonth(ym, TransactionType.Income, 100_000L)
        stubMonth(ym, TransactionType.Spend, 500_000L)

        val result = useCase(months = 6, clock = fixedClock)

        assertEquals(0, result.currentRatePercent)
    }

    @Test
    fun `null delta when prior period has no income`() = runTest {
        stubAllMonthsBlank(12)
        var ym = YearMonth(2026, Month.MAY)
        repeat(6) {
            stubMonth(ym, TransactionType.Income, 600_000L)
            stubMonth(ym, TransactionType.Spend, 400_000L)
            ym = ym.previous()
        }

        val result = useCase(months = 6, clock = fixedClock)

        assertNull(result.deltaPointsVsPrior)
    }

    @Test
    fun `monthly list has oldest month first`() = runTest {
        stubAllMonthsBlank(12)

        val result = useCase(months = 6, clock = fixedClock)

        assertEquals(6, result.monthly.size)
        assertEquals(Month.DECEMBER, result.monthly.first().yearMonth.month)
        assertEquals(Month.MAY, result.monthly.last().yearMonth.month)
    }

    @Test
    fun `averages are total divided by months count`() = runTest {
        stubAllMonthsBlank()
        val ym = YearMonth(2026, Month.MAY)
        stubMonth(ym, TransactionType.Income, 60_000L)
        stubMonth(ym, TransactionType.Spend, 40_000L)

        val result = useCase(months = 6, clock = fixedClock)

        assertEquals(Money(10_000L), result.averageIncome) // 60000 / 6
        assertEquals(Money(6_666L), result.averageExpense) // 40000 / 6 = 6666
    }
}
