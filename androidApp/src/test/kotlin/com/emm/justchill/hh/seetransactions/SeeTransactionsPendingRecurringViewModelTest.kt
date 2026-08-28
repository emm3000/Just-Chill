package com.emm.justchill.hh.seetransactions

import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
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
import com.emm.justchill.core.time.FakeTodayFlow
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
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Midday, so nothing here depends on where a day boundary falls. */
private val NOON = LocalTime(12, 0)

/** Mid-month, so nothing here depends on where a month boundary falls. */
private val TODAY = LocalDate(2026, 8, 15)

/**
 * The pending-recurring wiring `SeeTransactionsViewModel` owns: sourcing, the visibility rule,
 * confirm, skip, and the refresh into the list below. Split out of SeeTransactionsViewModelTest so
 * neither class trips detekt's LargeClass.
 */
@Suppress("IgnoredReturnValue")
class SeeTransactionsPendingRecurringViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

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

    private fun buildViewModel(today: MutableStateFlow<LocalDate> = MutableStateFlow(TODAY)) = SeeTransactionsViewModel(
        categoryRepository,
        transactionRepository,
        getPendingRecurringMovements,
        confirmRecurring,
        skipRecurring,
        FakeTodayFlow(today),
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
        accountName = "BCP",
        category = null,
    )

    @Test
    fun `pending list is sourced from GetPendingRecurringMovementsUseCase with whatever date todayFlow reports`() =
        runTest(testDispatcher) {
            // Zone-correct date derivation is ClockTodayFlow's job, pinned in ClockTodayFlowTest;
            // this only checks the ViewModel forwards todayFlow's value to the use case unchanged.
            val today = MutableStateFlow(LocalDate(2026, 8, 31))

            buildViewModel(today)
            advanceUntilIdle()

            verify { getPendingRecurringMovements(LocalDate(2026, 8, 31)) }
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
    fun `a period older than today's month is marked catch-up, today's own month is not`() = runTest(testDispatcher) {
        pendingFlow.value = listOf(
            pending("rm-old", "Netflix", period = YearMonth(2026, Month.MARCH)),
            pending("rm-now", "Spotify", period = period),
        )
        val vm = buildViewModel()
        advanceUntilIdle()

        val items = vm.state.value.pendingRecurringMovements
        assertTrue(items.single { it.templateId == "rm-old" }.isCatchUp, "March 2026 is a caught-up month")
        assertFalse(items.single { it.templateId == "rm-now" }.isCatchUp, "August 2026 is today's own month")
    }

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
    fun `filtered by category, current month, with pending hides the pending section`() = runTest(testDispatcher) {
        categoriesFlow.value = listOf(
            Category(
                categoryId = CategoryId("cat-1"),
                name = "Comida",
                icon = "icon",
                color = "green",
                categoryType = CategoryType.Spend,
            ),
        )
        pendingFlow.value = listOf(pending())
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
        advanceUntilIdle()

        assertTrue(vm.state.value.isFilterActive)
        assertFalse(vm.state.value.isPendingSectionVisible)
    }

    @Test
    fun `pending section stays visible when the month has no transactions yet (EmptyMonth)`() =
        runTest(testDispatcher) {
            pendingFlow.value = listOf(pending())
            totalsFlow.value = TransactionTotals(balance = Money(10_000), movementCount = 3)
            val vm = buildViewModel()
            advanceUntilIdle()

            assertEquals(ListDisplayState.EmptyMonth, vm.state.value.listDisplayState)
            assertTrue(vm.state.value.isPendingSectionVisible)
        }

    @Test
    fun `pending section stays visible when the whole ledger is empty (EmptyLedger)`() = runTest(testDispatcher) {
        pendingFlow.value = listOf(pending())
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(ListDisplayState.EmptyLedger, vm.state.value.listDisplayState)
        assertTrue(vm.state.value.isPendingSectionVisible)
    }

    @Test
    fun `pending recurring does not re-query when only the browsed month changes`() = runTest(testDispatcher) {
        // Pendings never depended on the browsed month — driven by todayFlow instead, so arrowing
        // through months must not cancel and re-subscribe the pending source.
        val vm = buildViewModel()
        advanceUntilIdle()
        verify(exactly = 1) { getPendingRecurringMovements(any()) }

        vm.onIntent(SeeTransactionsIntent.OnNextMonth)
        advanceUntilIdle()
        vm.onIntent(SeeTransactionsIntent.OnPreviousMonth)
        advanceUntilIdle()

        verify(exactly = 1) { getPendingRecurringMovements(any()) }
    }

    @Test
    fun `a recurring movement due at midnight appears with no user interaction`() = runTest(testDispatcher) {
        // Only the 16th's query answers with the movement, the way the real use case does: the
        // assertion below can pass only if the rollover re-queried with the new date.
        every { getPendingRecurringMovements(LocalDate(2026, 8, 16)) } returns MutableStateFlow(listOf(pending()))
        val today = MutableStateFlow(LocalDate(2026, 8, 15))
        val vm = buildViewModel(today)
        advanceUntilIdle()
        assertTrue(vm.state.value.pendingRecurringMovements.isEmpty())

        today.value = LocalDate(2026, 8, 16)
        advanceUntilIdle()

        assertEquals(1, vm.state.value.pendingRecurringMovements.size, "the newly due movement must appear")
    }

    @Test
    fun `ConfirmRecurring with fixed amount calls the use case and closes the confirm sheet`() =
        runTest(testDispatcher) {
            coEvery { confirmRecurring(any(), any(), any()) } returns Unit
            val vm = buildViewModel()
            vm.onIntent(SeeTransactionsIntent.OnPendingClicked("rm-1"))

            vm.onIntent(SeeTransactionsIntent.ConfirmRecurring("rm-1", period, Money(1800L)))
            advanceUntilIdle()

            coVerify(exactly = 1) { confirmRecurring(RecurringMovementId("rm-1"), period, Money(1800L)) }
            assertNull(vm.state.value.confirmSheetPendingId)
        }

    @Test
    fun `OnPendingClicked opens the confirm sheet for that pending id`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnPendingClicked("rm-1"))

        assertEquals("rm-1", vm.state.value.confirmSheetPendingId)
    }

    @Test
    fun `OnConfirmSheetDismissed closes the sheet without touching the use cases`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(SeeTransactionsIntent.OnPendingClicked("rm-1"))

        vm.onIntent(SeeTransactionsIntent.OnConfirmSheetDismissed)

        assertNull(vm.state.value.confirmSheetPendingId)
        coVerify(exactly = 0) { confirmRecurring(any(), any(), any()) }
        coVerify(exactly = 0) { skipRecurring(any(), any()) }
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
    fun `SkipRecurring settles the period and closes the confirm sheet`() = runTest(testDispatcher) {
        coEvery { skipRecurring(any(), any()) } returns Unit
        val vm = buildViewModel()
        vm.onIntent(SeeTransactionsIntent.OnPendingClicked("rm-1"))

        vm.onIntent(SeeTransactionsIntent.SkipRecurring("rm-1", period))
        advanceUntilIdle()

        coVerify(exactly = 1) { skipRecurring(RecurringMovementId("rm-1"), period) }
        coVerify(exactly = 0) { confirmRecurring(any(), any(), any()) }
        assertNull(vm.state.value.confirmSheetPendingId)
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
