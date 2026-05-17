package com.emm.domain.home

import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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
        assertEquals(Money(700L), data.income)
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
}
