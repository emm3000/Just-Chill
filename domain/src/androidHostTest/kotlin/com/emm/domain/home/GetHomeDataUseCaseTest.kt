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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class GetHomeDataUseCaseTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val getPendingRecurringMovements = mockk<GetPendingRecurringMovementsUseCase>()

    private val useCase = GetHomeDataUseCase(
        transactionRepository,
        getPendingRecurringMovements,
        fixedClock("2026-05-16T12:34:56Z"),
        TimeZone.UTC,
    )

    private fun tx(
        id: String,
        type: TransactionType,
        amount: Long,
        occurredAt: LocalDateTime = LocalDateTime(2026, 5, 16, 0, 0),
    ) = TransactionWithCategory(
        transactionId = TransactionId(id),
        type = type,
        amount = Money(amount),
        description = "",
        occurredAt = occurredAt,
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
        val startSlot = slot<String>()
        val endSlot = slot<String>()

        every { repo.observeTotals() } returns flowOf(TransactionTotals.Empty)
        every { repo.fetchAllWithCategoryInRange(capture(startSlot), capture(endSlot)) } returns flowOf(emptyList())
        every { pendingUc(any()) } returns flowOf(emptyList())

        GetHomeDataUseCase(repo, pendingUc, fixedClock, TimeZone.UTC).invoke().first()

        assertEquals("2026-05-01", startSlot.captured)
        assertEquals("2026-06-01", endSlot.captured)
    }

    @Test
    fun `the default month is read in the injected zone, not the device's`() = runTest {
        assertEquals("2026-05-01", startOfDefaultWindow(zoneOffsetHours = -5))
        assertEquals("2026-06-01", startOfDefaultWindow(zoneOffsetHours = 0))
    }

    private suspend fun startOfDefaultWindow(zoneOffsetHours: Int): String {
        val repo = mockk<TransactionRepository>()
        val pendingUc = mockk<GetPendingRecurringMovementsUseCase>()
        val startSlot = slot<String>()

        every { repo.observeTotals() } returns flowOf(TransactionTotals.Empty)
        every { repo.fetchAllWithCategoryInRange(capture(startSlot), any()) } returns flowOf(emptyList())
        every { pendingUc(any()) } returns flowOf(emptyList())

        GetHomeDataUseCase(
            repo,
            pendingUc,
            fixedClock("2026-06-01T02:00:00Z"),
            UtcOffset(hours = zoneOffsetHours).asTimeZone(),
        ).invoke().first()

        return startSlot.captured
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
