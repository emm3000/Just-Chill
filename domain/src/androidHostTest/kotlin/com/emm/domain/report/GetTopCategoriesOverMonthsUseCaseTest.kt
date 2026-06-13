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
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class GetTopCategoriesOverMonthsUseCaseTest {

    private val repository = mockk<TransactionStatsRepository>()
    private val useCase = GetTopCategoriesOverMonthsUseCase(repository)

    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-05-15T12:00:00Z")
    }

    private fun cat(id: String, amountCents: Long) = CategoryAmount(
        categoryId = CategoryId(id),
        categoryName = "Cat $id",
        categoryIcon = "icon_$id",
        categoryColor = "blue",
        amount = Money(amountCents),
    )

    private fun stubMonthEmpty(ym: YearMonth) {
        coEvery {
            repository.monthlyAmountByCategory(any(), ym.startInclusiveMillis(), ym.endExclusiveMillis())
        } returns emptyList()
    }

    private fun stubAllMonthsEmpty(months: Int = 6) {
        var ym = YearMonth(2026, Month.MAY)
        repeat(months) {
            stubMonthEmpty(ym)
            ym = ym.previous()
        }
    }

    private fun stubMonth(ym: YearMonth, items: List<CategoryAmount>) {
        coEvery {
            repository.monthlyAmountByCategory(any(), ym.startInclusiveMillis(), ym.endExclusiveMillis())
        } returns items
    }

    @Test
    fun `happy path - returns top 3 categories sorted by total amount`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        stubAllMonthsEmpty(6)
        stubMonth(may, listOf(cat("A", 500_00L), cat("B", 300_00L), cat("C", 200_00L)))

        val result = useCase(TransactionType.Spend, months = 1, topN = 3, clock = fixedClock)

        assertEquals(3, result.size)
        assertEquals(CategoryId("A"), result[0].categoryId)
        assertEquals(CategoryId("B"), result[1].categoryId)
        assertEquals(CategoryId("C"), result[2].categoryId)
    }

    @Test
    fun `empty months returns empty list`() = runTest {
        stubAllMonthsEmpty(6)

        val result = useCase(TransactionType.Spend, months = 6, topN = 3, clock = fixedClock)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `monthsInTop is counted correctly`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        val apr = may.previous()

        stubMonth(may, listOf(cat("A", 500_00L), cat("B", 300_00L)))
        stubMonth(apr, listOf(cat("A", 400_00L)))

        val result = useCase(TransactionType.Spend, months = 2, topN = 3, clock = fixedClock)

        val catA = result.first { it.categoryId == CategoryId("A") }
        val catB = result.first { it.categoryId == CategoryId("B") }

        assertEquals(2, catA.monthsInTop)
        assertEquals(1, catB.monthsInTop)
        assertEquals(2, catA.totalMonths)
    }

    @Test
    fun `partial data - only one month has data`() = runTest {
        stubAllMonthsEmpty(6)
        val may = YearMonth(2026, Month.MAY)
        stubMonth(may, listOf(cat("A", 1000_00L)))

        val result = useCase(TransactionType.Spend, months = 6, topN = 3, clock = fixedClock)

        assertEquals(1, result.size)
        assertEquals(Money(1000_00L), result[0].totalAmount)
        assertEquals(1, result[0].monthsInTop)
        assertEquals(6, result[0].totalMonths)
    }

    @Test
    fun `returns at most topN results`() = runTest {
        val may = YearMonth(2026, Month.MAY)
        stubMonth(may, listOf(cat("A", 500_00L), cat("B", 400_00L), cat("C", 300_00L), cat("D", 200_00L)))

        val result = useCase(TransactionType.Spend, months = 1, topN = 2, clock = fixedClock)

        assertEquals(2, result.size)
    }
}
