package com.emm.domain.home

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetHomeDataUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val useCase = GetHomeDataUseCase(transactionRepository)

    private fun tx(
        id: String,
        type: TransactionType,
        amount: Long,
        date: Long = 0L,
    ) = TransactionWithCategory(
        transactionId = TransactionId(id),
        type = type,
        amount = Money(amount),
        description = "",
        date = date,
        accountId = AccountId("acc-1"),
        category = null,
    )

    @Test
    fun `invoke should compute income, spend and balance from the right sources`() = runTest {
        val currentMonth = listOf(
            tx("1", TransactionType.Income, 10000L),
            tx("2", TransactionType.Spend, 3000L),
            tx("3", TransactionType.Income, 5000L),
        )
        val allTransactions = currentMonth + listOf(
            // previous month leftover that should only affect balance
            tx("legacy", TransactionType.Spend, 20000L),
        )
        every { transactionRepository.fetchAllWithCategory() } returns flowOf(allTransactions)
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(currentMonth)

        val data = useCase().first()

        assertEquals(Money(15000L), data.income)
        assertEquals(Money(3000L), data.spend)
        // balance = 10000 + 5000 - 3000 - 20000 = -8000 cents
        assertEquals(Money(-8000L), data.balance)
        assertEquals(currentMonth, data.lastTransactions)
    }

    @Test
    fun `invoke should take only the first seven transactions in lastTransactions`() = runTest {
        val currentMonth = (1..10).map { tx(it.toString(), TransactionType.Income, 100L) }
        every { transactionRepository.fetchAllWithCategory() } returns flowOf(currentMonth)
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(currentMonth)

        val data = useCase().first()

        assertEquals(7, data.lastTransactions.size)
        // income is summed from all current-month transactions, not just the display slice
        assertEquals(Money(1000L), data.income)
    }

    @Test
    fun `invoke should return zeros when nothing is in the current month`() = runTest {
        every { transactionRepository.fetchAllWithCategory() } returns flowOf(emptyList())
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(emptyList())

        val data = useCase().first()

        assertEquals(Money.Zero, data.income)
        assertEquals(Money.Zero, data.spend)
        assertEquals(Money.Zero, data.balance)
        assertTrue(data.lastTransactions.isEmpty())
    }

    @Test
    fun `integer arithmetic avoids floating-point rounding for Money sums`() = runTest {
        // 3 transactions of 33 cents each must sum to exactly 99 cents, not 98 or 100
        val currentMonth = listOf(
            tx("a", TransactionType.Income, 33L),
            tx("b", TransactionType.Income, 33L),
            tx("c", TransactionType.Income, 33L),
        )
        every { transactionRepository.fetchAllWithCategory() } returns flowOf(currentMonth)
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(currentMonth)

        val data = useCase().first()

        assertEquals(Money(99L), data.income)
    }

    @Test
    fun `currentMonthRange uses injected clock to compute start of month`() = runTest {
        val fixedClock = fixedClock("2026-05-16T12:34:56Z")
        val repo = mockk<TransactionRepository>()
        val startSlot = slot<Long>()
        val endSlot = slot<Long>()

        every { repo.fetchAllWithCategory() } returns flowOf(emptyList())
        every { repo.fetchAllWithCategoryInRange(capture(startSlot), capture(endSlot)) } returns flowOf(emptyList())

        GetHomeDataUseCase(repo, fixedClock).invoke().first()

        val zone = TimeZone.currentSystemDefault()
        val expectedStart = LocalDate(2026, 5, 1).atStartOfDayIn(zone).toEpochMilliseconds()
        val expectedEnd = LocalDate(2026, 6, 1).atStartOfDayIn(zone).toEpochMilliseconds()

        assertEquals(expectedStart, startSlot.captured)
        assertEquals(expectedEnd, endSlot.captured)
    }

    private fun fixedClock(at: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(at)
    }
}
