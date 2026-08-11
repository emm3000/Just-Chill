package com.emm.justchill.hh.recurring

import com.emm.domain.recurring.DeleteRecurringMovementUseCase
import com.emm.domain.recurring.GetAllRecurringMovementDetailsUseCase
import com.emm.domain.recurring.GetRecurringMonthlyTotalsUseCase
import com.emm.domain.recurring.RecurringMonthlyTotals
import com.emm.domain.recurring.RecurringMovementDetails
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecurringMovementsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val getAllDetails = mockk<GetAllRecurringMovementDetailsUseCase>()
    private val getTotals = mockk<GetRecurringMonthlyTotalsUseCase>()
    private val deleteRecurring = mockk<DeleteRecurringMovementUseCase>()

    private lateinit var viewModel: RecurringMovementsViewModel

    // A test data factory: every parameter past the first two is a defaulted knob one test flips.
    @Suppress("LongParameterList")
    private fun details(
        id: String,
        name: String,
        type: TransactionType = TransactionType.Spend,
        amount: Money? = Money(1800L),
        isActive: Boolean = true,
        dayOfMonth: Int = 15,
    ) = RecurringMovementDetails(
        id = id,
        name = name,
        type = type,
        amount = amount,
        categoryName = null,
        categoryColor = null,
        accountName = "BCP",
        dayOfMonth = dayOfMonth,
        isActive = isActive,
    )

    @Before
    fun setUp() {
        every { getAllDetails() } returns flowOf(emptyList())
        every { getTotals(any()) } returns RecurringMonthlyTotals.Empty
        viewModel = RecurringMovementsViewModel(getAllDetails, getTotals, deleteRecurring)
    }

    // ---- R7.1 — Active and paused templates partitioned correctly and sorted ----

    @Test
    fun `R7_1 active and paused items are partitioned and sorted by day of month`() = runTest {
        every { getAllDetails() } returns flowOf(
            listOf(
                details("rm-a", "Netflix", isActive = true, dayOfMonth = 3),
                details("rm-b", "Agua", isActive = false, dayOfMonth = 20),
                details("rm-c", "Alquiler", isActive = true, dayOfMonth = 5),
            ),
        )
        every { getTotals(any()) } returns RecurringMonthlyTotals.Empty
        viewModel = RecurringMovementsViewModel(getAllDetails, getTotals, deleteRecurring)

        advanceUntilIdle()

        // Day order (Netflix=3, Alquiler=5) differs from name order, proving the sort key is the day.
        val state = viewModel.state.value
        assertEquals(listOf("Netflix", "Alquiler"), state.activeItems.map { it.name })
        assertEquals(listOf("Agua"), state.pausedItems.map { it.name })
    }

    // ---- R7.2 — All active, pausedItems empty ----

    @Test
    fun `R7_2 all active templates result in empty pausedItems`() = runTest {
        every { getAllDetails() } returns flowOf(
            listOf(
                details("rm-1", "Netflix", isActive = true),
                details("rm-2", "Spotify", isActive = true),
            ),
        )
        every { getTotals(any()) } returns RecurringMonthlyTotals.Empty
        viewModel = RecurringMovementsViewModel(getAllDetails, getTotals, deleteRecurring)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.activeItems.size)
        assertTrue(state.pausedItems.isEmpty())
    }

    // ---- R7.3 — All paused, activeItems empty ----

    @Test
    fun `R7_3 all paused templates result in empty activeItems`() = runTest {
        every { getAllDetails() } returns flowOf(
            listOf(
                details("rm-1", "Netflix", isActive = false),
                details("rm-2", "Agua", isActive = false),
            ),
        )
        every { getTotals(any()) } returns RecurringMonthlyTotals.Empty
        viewModel = RecurringMovementsViewModel(getAllDetails, getTotals, deleteRecurring)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.activeItems.isEmpty())
        assertEquals(2, state.pausedItems.size)
    }

    // ---- R8.2 / R12.1 — Empty list: formatted defaults, variableCount = 0 ----

    @Test
    fun `R8_2 and R12_1 empty list yields empty state with zero variableCount`() = runTest {
        every { getAllDetails() } returns flowOf(emptyList())
        every { getTotals(any()) } returns RecurringMonthlyTotals.Empty
        viewModel = RecurringMovementsViewModel(getAllDetails, getTotals, deleteRecurring)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.activeItems.isEmpty())
        assertTrue(state.pausedItems.isEmpty())
        assertEquals(0, state.variableCount)
    }

    // ---- R9.1 — variableCount propagated to state ----

    @Test
    fun `R9_1 variableCount is propagated from totals to state`() = runTest {
        every { getAllDetails() } returns flowOf(
            listOf(details("rm-1", "Salario", amount = null, isActive = true)),
        )
        every { getTotals(any()) } returns RecurringMonthlyTotals(Money.Zero, Money.Zero, activeVariableCount = 1)
        viewModel = RecurringMovementsViewModel(getAllDetails, getTotals, deleteRecurring)

        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.variableCount)
    }

    // ---- R8.3 — Neutral-formatted amounts (no sign, with S/ symbol) ----

    @Test
    fun `R8_3 formatted amounts use neutral currency format without directional sign`() = runTest {
        every { getAllDetails() } returns flowOf(
            listOf(details("rm-1", "Sueldo", type = TransactionType.Income, amount = Money(350000L))),
        )
        every { getTotals(any()) } returns RecurringMonthlyTotals(
            incomeTotal = Money(350000L),
            expenseTotal = Money(180000L),
            activeVariableCount = 0,
        )
        viewModel = RecurringMovementsViewModel(getAllDetails, getTotals, deleteRecurring)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(
            state.entranFormatted.startsWith("S/"),
            "ENTRAN must start with 'S/' — got '${state.entranFormatted}'",
        )
        assertTrue(
            state.salenFormatted.startsWith("S/"),
            "SALEN must start with 'S/' — got '${state.salenFormatted}'",
        )
        assertTrue(
            !state.entranFormatted.contains("+"),
            "ENTRAN must not contain '+' — got '${state.entranFormatted}'",
        )
        assertTrue(
            !state.salenFormatted.contains("−"),
            "SALEN must not contain '−' — got '${state.salenFormatted}'",
        )
    }

    // ---- Existing intent tests (unchanged behaviors) ----

    @Test
    fun `NavigateToAdd dispatches NavigateToAddEdit(null) effect`() = runTest {
        val effects = mutableListOf<RecurringMovementsEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(RecurringMovementsIntent.NavigateToAdd)
        advanceUntilIdle()

        assertTrue(effects.any { it is RecurringMovementsEffect.NavigateToAddEdit && it.id == null })
        job.cancel()
    }

    @Test
    fun `NavigateToEdit dispatches NavigateToAddEdit with id effect`() = runTest {
        val effects = mutableListOf<RecurringMovementsEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(RecurringMovementsIntent.NavigateToEdit("rm-42"))
        advanceUntilIdle()

        assertTrue(effects.any { it is RecurringMovementsEffect.NavigateToAddEdit && it.id == "rm-42" })
        job.cancel()
    }

    @Test
    fun `RequestDelete sets pendingDelete in state`() = runTest {
        viewModel.onIntent(RecurringMovementsIntent.RequestDelete("rm-1"))
        advanceUntilIdle()

        assertEquals("rm-1", viewModel.state.value.pendingDelete)
    }

    @Test
    fun `DismissDelete clears pendingDelete`() = runTest {
        viewModel.onIntent(RecurringMovementsIntent.RequestDelete("rm-1"))
        viewModel.onIntent(RecurringMovementsIntent.DismissDelete)
        advanceUntilIdle()

        assertNull(viewModel.state.value.pendingDelete)
    }

    @Test
    fun `ConfirmDelete calls DeleteRecurringMovementUseCase and clears pendingDelete`() = runTest {
        coEvery { deleteRecurring(any()) } returns Unit
        viewModel.onIntent(RecurringMovementsIntent.RequestDelete("rm-1"))
        viewModel.onIntent(RecurringMovementsIntent.ConfirmDelete)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteRecurring(RecurringMovementId("rm-1")) }
        assertNull(viewModel.state.value.pendingDelete)
    }

    @Test
    fun `ConfirmDelete with NotFound propagates ShowError effect`() = runTest {
        coEvery { deleteRecurring(any()) } throws DomainException.NotFound("rm-ghost")
        viewModel.onIntent(RecurringMovementsIntent.RequestDelete("rm-ghost"))

        val effects = mutableListOf<RecurringMovementsEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(RecurringMovementsIntent.ConfirmDelete)
        advanceUntilIdle()

        assertTrue(effects.any { it is RecurringMovementsEffect.ShowError })
        job.cancel()
    }

    // ---- pendingDelete lookup searches both lists (regression guard) ----

    @Test
    fun `pendingDelete can reference an item in pausedItems`() = runTest {
        every { getAllDetails() } returns flowOf(
            listOf(
                details("active-1", "Netflix", isActive = true),
                details("paused-1", "Agua", isActive = false),
            ),
        )
        every { getTotals(any()) } returns RecurringMonthlyTotals.Empty
        viewModel = RecurringMovementsViewModel(getAllDetails, getTotals, deleteRecurring)
        advanceUntilIdle()

        viewModel.onIntent(RecurringMovementsIntent.RequestDelete("paused-1"))
        advanceUntilIdle()

        assertEquals("paused-1", viewModel.state.value.pendingDelete)
    }
}
