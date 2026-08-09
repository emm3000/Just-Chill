package com.emm.domain.home

import com.emm.domain.recurring.GetPendingRecurringMovementsUseCase
import com.emm.domain.recurring.PendingRecurring
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionTotals
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class GetHomeDataUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val getPendingRecurringMovements = mockk<GetPendingRecurringMovementsUseCase>()

    private val useCase = GetHomeDataUseCase(transactionRepository, getPendingRecurringMovements)

    private fun tx(id: String, type: TransactionType, amount: Long, date: Long = 0L) = TransactionWithCategory(
        transactionId = TransactionId(id),
        type = type,
        amount = Money(amount),
        description = "",
        date = date,
        accountId = AccountId("acc-1"),
        category = null,
    )

    private fun stub(
        currentMonth: List<TransactionWithCategory> = emptyList(),
        totals: TransactionTotals = TransactionTotals.Empty,
        pending: List<PendingRecurring> = emptyList(),
    ) {
        every { transactionRepository.observeTotals() } returns flowOf(totals)
        every { transactionRepository.fetchAllWithCategoryInRange(any(), any()) } returns flowOf(currentMonth)
        every { getPendingRecurringMovements(any()) } returns flowOf(pending)
    }

    @Test
    fun `invoke should compute income and spend from the current month`() = runTest {
        val currentMonth = listOf(
            tx("1", TransactionType.Income, 10000L),
            tx("2", TransactionType.Spend, 3000L),
            tx("3", TransactionType.Income, 5000L),
        )
        stub(currentMonth = currentMonth, totals = TransactionTotals(Money(-8000L), 4L))

        val data = useCase().first()

        assertEquals(Money(15000L), data.income)
        assertEquals(Money(3000L), data.spend)
        assertEquals(currentMonth, data.lastTransactions)
        assertTrue(data.pendingRecurringMovements.isEmpty())
    }

    @Test
    fun `balance comes from the aggregate, not from folding the whole table`() = runTest {
        // The month holds a single 100 income, but the ledger as a whole is 80 in the red. Folding
        // the current month — the only list the use case still reads — could never produce that.
        stub(
            currentMonth = listOf(tx("1", TransactionType.Income, 10000L)),
            totals = TransactionTotals(Money(-8000L), 4L),
        )

        val data = useCase().first()

        assertEquals(Money(-8000L), data.balance)
        verify(exactly = 0) { transactionRepository.fetchAllWithCategory() }
    }

    @Test
    fun `hasAnyTransaction follows the ledger count, not the visible month`() = runTest {
        // Browsing back to an empty month must not make the app think the user has never recorded
        // anything: that flips Home into its first-run empty state.
        stub(currentMonth = emptyList(), totals = TransactionTotals(Money(12000L), 3L))

        val data = useCase().first()

        assertTrue(data.hasAnyTransaction)
        assertTrue(data.lastTransactions.isEmpty())
    }

    @Test
    fun `invoke should take only the first seven transactions in lastTransactions`() = runTest {
        val currentMonth = (1..10).map { tx(it.toString(), TransactionType.Income, 100L) }
        stub(currentMonth = currentMonth, totals = TransactionTotals(Money(1000L), 10L))

        val data = useCase().first()

        assertEquals(7, data.lastTransactions.size)
        // income is summed from all current-month transactions, not just the display slice
        assertEquals(Money(1000L), data.income)
    }

    @Test
    fun `invoke should return zeros when nothing is recorded`() = runTest {
        stub()

        val data = useCase().first()

        assertEquals(Money.Zero, data.income)
        assertEquals(Money.Zero, data.spend)
        assertEquals(Money.Zero, data.balance)
        assertFalse(data.hasAnyTransaction)
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
        stub(currentMonth = currentMonth, totals = TransactionTotals(Money(99L), 3L))

        val data = useCase().first()

        assertEquals(Money(99L), data.income)
    }

    @Test
    fun `currentMonthRange uses injected clock to compute start of month`() = runTest {
        val fixedClock = fixedClock("2026-05-16T12:34:56Z")
        val repo = mockk<TransactionRepository>()
        val pendingUc = mockk<GetPendingRecurringMovementsUseCase>()
        val startSlot = slot<Long>()
        val endSlot = slot<Long>()

        every { repo.observeTotals() } returns flowOf(TransactionTotals.Empty)
        every { repo.fetchAllWithCategoryInRange(capture(startSlot), capture(endSlot)) } returns flowOf(emptyList())
        every { pendingUc(any()) } returns flowOf(emptyList())

        GetHomeDataUseCase(repo, pendingUc, fixedClock).invoke().first()

        val zone = TimeZone.currentSystemDefault()
        val expectedStart = LocalDate(2026, 5, 1).atStartOfDayIn(zone).toEpochMilliseconds()
        val expectedEnd = LocalDate(2026, 6, 1).atStartOfDayIn(zone).toEpochMilliseconds()

        assertEquals(expectedStart, startSlot.captured)
        assertEquals(expectedEnd, endSlot.captured)
    }

    @Test
    fun `invoke should include pending recurring movements in HomeData`() = runTest {
        val pendingItem = mockk<PendingRecurring>()
        stub(pending = listOf(pendingItem))

        val data = useCase().first()

        assertEquals(1, data.pendingRecurringMovements.size)
        assertEquals(pendingItem, data.pendingRecurringMovements.first())
    }

    private fun fixedClock(at: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(at)
    }
}
