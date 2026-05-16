package com.emm.domain.home

import com.emm.domain.account.AccountRepository
import com.emm.domain.shared.AccountId
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
    private val accountRepository = mockk<AccountRepository>()
    private val useCase = GetHomeDataUseCase(transactionRepository, accountRepository)

    private fun tx(
        id: String,
        type: TransactionType,
        amount: Double,
        date: Long = 0L,
    ) = TransactionWithCategory(
        transactionId = TransactionId(id),
        type = type,
        amount = amount,
        description = "",
        date = date,
        accountId = AccountId("acc-1"),
        category = null,
    )

    @Test
    fun `invoke should compute income, spend and balance from the right sources`() = runTest {
        val currentMonth = listOf(
            tx("1", TransactionType.Income, 100.0),
            tx("2", TransactionType.Spend, 30.0),
            tx("3", TransactionType.Income, 50.0),
        )
        val allTransactions = currentMonth + listOf(
            // previous month leftover that should only affect balance
            tx("legacy", TransactionType.Spend, 200.0),
        )
        every { accountRepository.all() } returns flowOf(emptyList())
        every { transactionRepository.fetchAllWithCategory() } returns flowOf(allTransactions)
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(currentMonth)

        val data = useCase().first()

        assertEquals(150.0, data.income, 0.0001)
        assertEquals(30.0, data.spend, 0.0001)
        // balance = 100 + 50 - 30 - 200 = -80
        assertEquals(-80.0, data.balance, 0.0001)
        assertEquals(currentMonth, data.lastTransactions)
    }

    @Test
    fun `invoke should take only the first seven transactions in lastTransactions`() = runTest {
        val currentMonth = (1..10).map { tx(it.toString(), TransactionType.Income, 1.0) }
        every { accountRepository.all() } returns flowOf(emptyList())
        every { transactionRepository.fetchAllWithCategory() } returns flowOf(currentMonth)
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(currentMonth)

        val data = useCase().first()

        assertEquals(7, data.lastTransactions.size)
        assertEquals(7.0, data.income, 0.0001)
    }

    @Test
    fun `invoke should return zeros when nothing is in the current month`() = runTest {
        every { accountRepository.all() } returns flowOf(emptyList())
        every { transactionRepository.fetchAllWithCategory() } returns flowOf(emptyList())
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(emptyList())

        val data = useCase().first()

        assertEquals(0.0, data.income, 0.0001)
        assertEquals(0.0, data.spend, 0.0001)
        assertEquals(0.0, data.balance, 0.0001)
        assertTrue(data.lastTransactions.isEmpty())
    }
}
