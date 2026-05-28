package com.emm.justchill.hh.recurring

import com.emm.domain.recurring.DeleteRecurringMovementUseCase
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.GetAllRecurringMovementsUseCase
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.shared.AccountId
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

    private val getAllRecurring = mockk<GetAllRecurringMovementsUseCase>()
    private val deleteRecurring = mockk<DeleteRecurringMovementUseCase>()

    private lateinit var viewModel: RecurringMovementsViewModel

    private fun recurringMovement(
        id: String = "rm-1",
        name: String = "Netflix",
        type: TransactionType = TransactionType.Spend,
        amount: Money? = Money(1800L),
        dayOfMonth: Int = 15,
        isActive: Boolean = true,
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
        isActive = isActive,
        lastConfirmedPeriod = null,
    )

    @Before
    fun setUp() {
        every { getAllRecurring() } returns flowOf(emptyList())
        viewModel = RecurringMovementsViewModel(getAllRecurring, deleteRecurring)
    }

    @Test
    fun `list loads and maps to RecurringMovementUi`() = runTest {
        val items = listOf(
            recurringMovement("rm-1", "Netflix"),
            recurringMovement("rm-2", "Spotify", amount = null),
        )
        every { getAllRecurring() } returns flowOf(items)
        viewModel = RecurringMovementsViewModel(getAllRecurring, deleteRecurring)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.items.size)
        assertEquals("rm-1", state.items[0].id)
        assertEquals("rm-2", state.items[1].id)
        assertTrue(state.items[1].isVariableAmount)
    }

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
}
