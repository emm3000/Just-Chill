package com.emm.justchill.hh.seetransactions

import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.GetPendingRecurringMovementsUseCase
import com.emm.domain.recurring.PendingRecurring
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.SkipRecurringMovementUseCase
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.YearMonth
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionTotals
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Midday, so nothing here depends on where a day boundary falls. */
private val NOON = LocalTime(12, 0)

/**
 * The pending-recurring wiring `SeeTransactionsViewModel` gained when the section moved off Home
 * (E06-03): sourcing, the visibility rule, confirm, skip, and the refresh into the list below.
 * Split out of SeeTransactionsViewModelTest so neither class trips detekt's LargeClass.
 */
@Suppress("IgnoredReturnValue")
class SeeTransactionsPendingRecurringViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    /** Mid-month noon UTC: `YearMonth.current(fixedClock)` is August 2026 in every timezone. */
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-15T12:00:00Z")
    }
    private val period = YearMonth(2026, Month.AUGUST)

    private val categoriesFlow = MutableStateFlow(emptyList<Category>())
    private val usageCountsFlow = MutableStateFlow(emptyMap<CategoryId, Int>())
    private val totalsFlow = MutableStateFlow(TransactionTotals.Empty)
    private val monthTransactionsFlow = MutableStateFlow(emptyList<TransactionWithCategory>())
    private val pendingFlow = MutableStateFlow(emptyList<PendingRecurring>())

    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns categoriesFlow
    }
    private val transactionRepository = mockk<TransactionRepository> {
        every { observeCategoryUsageCounts() } returns usageCountsFlow
        every { observeTotals() } returns totalsFlow
        every { fetchAllWithCategoryInRange(any(), any()) } returns monthTransactionsFlow
        every { searchWithCategory(any()) } returns flowOf(emptyList())
    }
    private val getPendingRecurringMovements = mockk<GetPendingRecurringMovementsUseCase> {
        every { this@mockk(any()) } returns pendingFlow
    }
    private val confirmRecurring = mockk<ConfirmRecurringMovementUseCase>()
    private val skipRecurring = mockk<SkipRecurringMovementUseCase>()

    private fun buildViewModel(clock: Clock = fixedClock, zone: TimeZone = TimeZone.UTC) = SeeTransactionsViewModel(
        categoryRepository,
        transactionRepository,
        getPendingRecurringMovements,
        confirmRecurring,
        skipRecurring,
        clock,
        zone,
    )

    private fun pending(
        id: String = "rm-1",
        name: String = "Netflix",
        amount: Money? = Money(1800L),
        period: YearMonth = this.period,
    ) = PendingRecurring(recurringMovement(id = id, name = name, amount = amount), period)

    private fun recurringMovement(
        id: String = "rm-1",
        name: String = "Netflix",
        type: TransactionType = TransactionType.Spend,
        amount: Money? = Money(1800L),
        dayOfMonth: Int = 15,
    ) = RecurringMovement(
        id = RecurringMovementId(id),
        name = name,
        type = type,
        amount = amount,
        description = "",
        categoryId = null,
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = dayOfMonth,
        isActive = true,
        lastConfirmedPeriod = null,
        createdAt = 0L,
    )

    private fun tx(id: String, type: TransactionType, cents: Long) = TransactionWithCategory(
        transactionId = TransactionId(id),
        type = type,
        amount = Money(cents),
        description = "movimiento $id",
        occurredAt = LocalDateTime(LocalDate(period.year, period.month, 5), NOON),
        accountId = AccountId("acc-1"),
        category = null,
    )

    @Test
    fun `pending list is sourced from GetPendingRecurringMovementsUseCase with today in the injected zone`() =
        runTest(testDispatcher) {
            val nearMidnight = object : Clock {
                override fun now(): Instant = Instant.parse("2026-09-01T02:00:00Z")
            }

            buildViewModel(nearMidnight, UtcOffset(hours = -5).asTimeZone())
            advanceUntilIdle()
            verify { getPendingRecurringMovements(LocalDate(2026, 8, 31)) }

            buildViewModel(nearMidnight, UtcOffset(hours = 0).asTimeZone())
            advanceUntilIdle()
            verify { getPendingRecurringMovements(LocalDate(2026, 9, 1)) }
        }

    @Test
    fun `two pending items map into pendingRecurringMovements`() = runTest(testDispatcher) {
        pendingFlow.value = listOf(pending("rm-1", "Netflix"), pending("rm-2", "Spotify", amount = null))
        val vm = buildViewModel()
        advanceUntilIdle()

        val items = vm.state.value.pendingRecurringMovements
        assertEquals(2, items.size)
        assertEquals("rm-1", items[0].templateId)
        assertEquals("rm-2", items[1].templateId)
        assertFalse(items[0].isVariableAmount)
        assertTrue(items[1].isVariableAmount)
    }

    @Test
    fun `a period older than the clock's month is marked catch-up, the clock's own month is not`() =
        runTest(testDispatcher) {
            pendingFlow.value = listOf(
                pending("rm-old", "Netflix", period = YearMonth(2026, Month.MARCH)),
                pending("rm-now", "Spotify", period = period),
            )
            val vm = buildViewModel()
            advanceUntilIdle()

            val items = vm.state.value.pendingRecurringMovements
            assertTrue(items.single { it.templateId == "rm-old" }.isCatchUp, "March 2026 is a caught-up month")
            assertFalse(items.single { it.templateId == "rm-now" }.isCatchUp, "August 2026 is the clock's own month")
        }

    // Visibility rule: unfiltered/filtered x current/other month, plus the nothing-pending case.

    @Test
    fun `unfiltered, current month, with pending shows the pending section`() = runTest(testDispatcher) {
        pendingFlow.value = listOf(pending())
        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.isPendingSectionVisible)
    }

    @Test
    fun `unfiltered, another month, with pending hides the pending section`() = runTest(testDispatcher) {
        pendingFlow.value = listOf(pending())
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnPreviousMonth)

        assertFalse(vm.state.value.isPendingSectionVisible)
    }

    @Test
    fun `filtered by search, current month, with pending hides the pending section`() = runTest(testDispatcher) {
        pendingFlow.value = listOf(pending())
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))

        assertFalse(vm.state.value.isPendingSectionVisible)
    }

    @Test
    fun `filtered by search, another month, with pending hides the pending section`() = runTest(testDispatcher) {
        pendingFlow.value = listOf(pending())
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnPreviousMonth)
        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))

        assertFalse(vm.state.value.isPendingSectionVisible)
    }

    @Test
    fun `unfiltered, current month, with nothing pending hides the pending section`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.pendingRecurringMovements.isEmpty())
        assertFalse(vm.state.value.isPendingSectionVisible)
    }

    @Test
    fun `ConfirmRecurring with fixed amount calls the use case and emits CloseConfirmSheet`() =
        runTest(testDispatcher) {
            coEvery { confirmRecurring(any(), any(), any()) } returns Unit
            val vm = buildViewModel()

            val effects = mutableListOf<SeeTransactionsEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(SeeTransactionsIntent.ConfirmRecurring("rm-1", period, Money(1800L)))
            advanceUntilIdle()

            coVerify(exactly = 1) { confirmRecurring(RecurringMovementId("rm-1"), period, Money(1800L)) }
            assertTrue(effects.any { it is SeeTransactionsEffect.CloseConfirmSheet })
            job.cancel()
        }

    @Test
    fun `ConfirmRecurring DomainException surfaces as ShowError carrying toUserMessage, not the raw exception`() =
        runTest(testDispatcher) {
            val error = DomainException.DatabaseError(RuntimeException("raw internal message"))
            coEvery { confirmRecurring(any(), any(), any()) } throws error
            val vm = buildViewModel()

            val effects = mutableListOf<SeeTransactionsEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(SeeTransactionsIntent.ConfirmRecurring("rm-1", period, Money(1800L)))
            advanceUntilIdle()

            val showError = effects.filterIsInstance<SeeTransactionsEffect.ShowError>().firstOrNull()
            checkNotNull(showError) { "Expected ShowError effect but got: $effects" }
            assertEquals("Hubo un problema guardando tu data", showError.message)
            assertFalse(showError.message.contains("raw internal message"))
            job.cancel()
        }

    @Test
    fun `SkipRecurring settles the period and emits CloseConfirmSheet`() = runTest(testDispatcher) {
        coEvery { skipRecurring(any(), any()) } returns Unit
        val vm = buildViewModel()

        val effects = mutableListOf<SeeTransactionsEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(SeeTransactionsIntent.SkipRecurring("rm-1", period))
        advanceUntilIdle()

        coVerify(exactly = 1) { skipRecurring(RecurringMovementId("rm-1"), period) }
        coVerify(exactly = 0) { confirmRecurring(any(), any(), any()) }
        assertTrue(effects.any { it is SeeTransactionsEffect.CloseConfirmSheet })
        job.cancel()
    }

    @Test
    fun `SkipRecurring surfaces a domain failure as ShowError`() = runTest(testDispatcher) {
        coEvery { skipRecurring(any(), any()) } throws DomainException.DatabaseError(RuntimeException("DB fail"))
        val vm = buildViewModel()

        val effects = mutableListOf<SeeTransactionsEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(SeeTransactionsIntent.SkipRecurring("rm-1", period))
        advanceUntilIdle()

        assertTrue(effects.any { it is SeeTransactionsEffect.ShowError })
        job.cancel()
    }

    @Test
    fun `confirming a pending movement leaves the transaction stream subscribed so the new row lands in state`() =
        runTest(testDispatcher) {
            coEvery { confirmRecurring(any(), any(), any()) } returns Unit
            val vm = buildViewModel()
            advanceUntilIdle()
            assertTrue(vm.state.value.days.isEmpty())

            vm.onIntent(SeeTransactionsIntent.ConfirmRecurring("rm-1", period, Money(1800L)))
            advanceUntilIdle()

            // The real use case writes the row via the same SQLDelight table the range query reads,
            // so its query re-emits on its own; this stands in for that reactive re-emission.
            monthTransactionsFlow.value = listOf(tx("t-confirmed", TransactionType.Spend, 1_800))
            advanceUntilIdle()

            assertEquals(
                listOf("t-confirmed"),
                vm.state.value.days.flatMap { d -> d.transactions.map { it.transactionId } },
            )
        }
}
