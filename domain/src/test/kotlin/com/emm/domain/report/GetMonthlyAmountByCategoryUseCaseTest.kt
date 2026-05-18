package com.emm.domain.report

import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Month
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetMonthlyAmountByCategoryUseCaseTest {

    private val repository = mockk<TransactionRepository>()
    private val useCase = GetMonthlyAmountByCategoryUseCase(repository)

    private val yearMonth = YearMonth(2026, Month.MAY)

    private fun cat(id: String, amountCents: Long) = CategoryAmount(
        categoryId = CategoryId(id),
        categoryName = "Cat $id",
        categoryIcon = "icon",
        categoryColor = "blue",
        amount = Money(amountCents),
    )

    @Test
    fun `returns list ordered descending by amount as provided by repository`() = runTest {
        val expected = listOf(cat("1", 4500_00L), cat("2", 1200_00L), cat("3", 400_00L))
        coEvery { repository.monthlyAmountByCategory(any(), any(), any()) } returns expected

        val result = useCase(yearMonth, TransactionType.Income)

        assertEquals(expected, result)
        assertEquals(4500_00L, result[0].amount.cents)
        assertEquals(1200_00L, result[1].amount.cents)
        assertEquals(400_00L, result[2].amount.cents)
    }

    @Test
    fun `returns empty list when month has no transactions`() = runTest {
        coEvery { repository.monthlyAmountByCategory(any(), any(), any()) } returns emptyList()

        val result = useCase(yearMonth, TransactionType.Income)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns single element when only one category has transactions`() = runTest {
        val single = listOf(cat("sueldo", 6200_00L))
        coEvery { repository.monthlyAmountByCategory(any(), any(), any()) } returns single

        val result = useCase(yearMonth, TransactionType.Income)

        assertEquals(1, result.size)
        assertEquals(CategoryId("sueldo"), result[0].categoryId)
        assertEquals(Money(6200_00L), result[0].amount)
    }

    @Test
    fun `passes correct type and range to repository`() = runTest {
        coEvery { repository.monthlyAmountByCategory(any(), any(), any()) } returns emptyList()

        useCase(yearMonth, TransactionType.Spend)

        val expectedStart = yearMonth.startInclusiveMillis()
        val expectedEnd = yearMonth.endExclusiveMillis()
        coVerify(exactly = 1) {
            repository.monthlyAmountByCategory(TransactionType.Spend, expectedStart, expectedEnd)
        }
    }
}
