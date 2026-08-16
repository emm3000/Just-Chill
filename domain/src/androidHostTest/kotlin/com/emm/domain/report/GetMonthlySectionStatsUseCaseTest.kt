package com.emm.domain.report

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

class GetMonthlySectionStatsUseCaseTest {

    private val repository = mockk<TransactionStatsRepository>()
    private val useCase = GetMonthlySectionStatsUseCase(repository)

    private val month = YearMonth(2026, Month.MAY)

    private fun stubStats(count: Long, totalCents: Long) {
        val average = if (count == 0L) Money.Zero else Money(totalCents / count)
        coEvery {
            repository.monthlyStats(any(), month.startInclusiveDay(), month.endExclusiveDay())
        } returns MonthlySectionStats(movementCount = count.toInt(), averageAmount = average)
    }

    @Test
    fun `returns count and average for normal case`() = runTest {
        stubStats(count = 4, totalCents = 620_000L)

        val result = useCase(month, TransactionType.Income)

        assertEquals(4, result.movementCount)
        assertEquals(Money(155_000L), result.averageAmount)
    }

    @Test
    fun `returns zero average when count is zero`() = runTest {
        stubStats(count = 0, totalCents = 0L)

        val result = useCase(month, TransactionType.Income)

        assertEquals(0, result.movementCount)
        assertEquals(Money.Zero, result.averageAmount)
    }

    @Test
    fun `returns correct average for a single movement`() = runTest {
        stubStats(count = 1, totalCents = 500_00L)

        val result = useCase(month, TransactionType.Spend)

        assertEquals(1, result.movementCount)
        assertEquals(Money(500_00L), result.averageAmount)
    }

    @Test
    fun `truncates fractional cents in average`() = runTest {
        stubStats(count = 3, totalCents = 100L)

        val result = useCase(month, TransactionType.Income)

        assertEquals(3, result.movementCount)
        assertEquals(Money(33L), result.averageAmount)
    }
}
