package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetMonthlyComparisonUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val useCase = GetMonthlyComparisonUseCase(repository)

    private val currentMonth = YearMonth(2026, Month.MAY)
    private val previousMonth = currentMonth.previous() // April 2026

    private fun stubMonthly(yearMonth: YearMonth, amountCents: Long) {
        val items = if (amountCents == 0L) emptyList()
        else listOf(
            CategoryAmount(
                categoryId = CategoryId("x"),
                categoryName = "X",
                categoryIcon = "icon",
                categoryColor = "blue",
                amount = Money(amountCents),
            )
        )
        coEvery {
            repository.monthlyAmountByCategory(
                any(),
                yearMonth.startInclusiveMillis(),
                yearMonth.endExclusiveMillis(),
            )
        } returns items
    }

    @Test
    fun `returns positive delta when current is greater than previous`() = runTest {
        stubMonthly(currentMonth, 6200_00L)
        stubMonthly(previousMonth, 5500_00L)

        val result = useCase(currentMonth, TransactionType.Income)

        requireNotNull(result)
        assertEquals(Money(6200_00L), result.currentTotal)
        assertEquals(Money(5500_00L), result.previousTotal)
        // delta = ((6200 - 5500) / 5500) * 100 = 12 (truncated)
        assertEquals(12, result.deltaPercent)
    }

    @Test
    fun `returns negative delta when current is less than previous`() = runTest {
        stubMonthly(currentMonth, 4000_00L)
        stubMonthly(previousMonth, 5000_00L)

        val result = useCase(currentMonth, TransactionType.Income)

        requireNotNull(result)
        assertEquals(-20, result.deltaPercent)
    }

    @Test
    fun `returns null when previous month total is zero`() = runTest {
        stubMonthly(currentMonth, 3000_00L)
        stubMonthly(previousMonth, 0L)

        val result = useCase(currentMonth, TransactionType.Income)

        assertNull(result)
    }

    @Test
    fun `returns minus 100 percent delta when current is zero but previous is not`() = runTest {
        stubMonthly(currentMonth, 0L)
        stubMonthly(previousMonth, 5000_00L)

        val result = useCase(currentMonth, TransactionType.Income)

        requireNotNull(result)
        assertEquals(Money.Zero, result.currentTotal)
        assertEquals(-100, result.deltaPercent)
    }

    @Test
    fun `edge case january wraps correctly to december of previous year`() = runTest {
        val january = YearMonth(2026, Month.JANUARY)
        val december2025 = january.previous() // December 2025

        stubMonthly(january, 6000_00L)
        stubMonthly(december2025, 5000_00L)

        val result = useCase(january, TransactionType.Income)

        requireNotNull(result)
        assertEquals(20, result.deltaPercent)
    }
}
