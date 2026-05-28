package com.emm.justchill.hh.home

import com.emm.domain.home.GetHomeDataUseCase
import com.emm.domain.home.HomeData
import com.emm.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.domain.recurring.Frequency
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val getHomeData = mockk<GetHomeDataUseCase>()
    private val confirmRecurring = mockk<ConfirmRecurringMovementUseCase>()

    private lateinit var viewModel: HomeViewModel

    private val emptyHomeData = HomeData(
        lastTransactions = emptyList(),
        income = Money.Zero,
        spend = Money.Zero,
        balance = Money.Zero,
        hasAnyTransaction = false,
        pendingRecurringMovements = emptyList(),
    )

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
    )

    @Before
    fun setUp() {
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = HomeViewModel(getHomeData, confirmRecurring)
    }

    // ---- Scenario 10.1: No pending → section absent ----

    @Test
    fun `10_1 empty pending list maps to empty pendingRecurringMovements in state`() = runTest {
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = HomeViewModel(getHomeData, confirmRecurring)

        advanceUntilIdle()

        assertTrue(viewModel.state.value.pendingRecurringMovements.isEmpty())
    }

    // ---- Scenario 10.2: One or more pending → section visible ----

    @Test
    fun `10_2 two pending items map to two PendingRecurringUi entries`() = runTest {
        val homeData = emptyHomeData.copy(
            pendingRecurringMovements = listOf(
                recurringMovement("rm-1", "Netflix"),
                recurringMovement("rm-2", "Spotify", amount = null),
            ),
        )
        every { getHomeData(any()) } returns flowOf(homeData)
        viewModel = HomeViewModel(getHomeData, confirmRecurring)

        advanceUntilIdle()

        val pending = viewModel.state.value.pendingRecurringMovements
        assertEquals(2, pending.size)
        assertEquals("rm-1", pending[0].templateId)
        assertEquals("rm-2", pending[1].templateId)
        assertFalse(pending[0].isVariableAmount)
        assertTrue(pending[1].isVariableAmount)
    }

    // ---- Scenario 4.1: Confirm fixed amount successfully ----

    @Test
    fun `4_1 ConfirmRecurring intent with fixed amount calls use case and emits CloseSheet effect`() = runTest {
        coEvery { confirmRecurring(any(), any(), any(), any()) } returns Unit
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = HomeViewModel(getHomeData, confirmRecurring)

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", Money(1800L)))
        advanceUntilIdle()

        coVerify(exactly = 1) { confirmRecurring(any(), any(), any(), eq(Money(1800L))) }
        assertTrue(effects.any { it is HomeEffect.CloseConfirmSheet })
        job.cancel()
    }

    // ---- Scenario 4.2: Confirm fails with DatabaseError ----

    @Test
    fun `4_2 ConfirmRecurring propagates DomainException as ShowSnackbar effect`() = runTest {
        val error = DomainException.DatabaseError(RuntimeException("DB fail"))
        coEvery { confirmRecurring(any(), any(), any(), any()) } throws error
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = HomeViewModel(getHomeData, confirmRecurring)

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", Money(1800L)))
        advanceUntilIdle()

        assertTrue(effects.any { it is HomeEffect.ShowError })
        job.cancel()
    }

    // ---- Scenario 5.1: Variable amount — confirm button disabled while amount == 0 ----
    // This scenario is enforced by the Sheet composable (amount field required before enabling the
    // button). The ViewModel receives a valid callerAmount when the user taps Confirm, so there is
    // no ViewModel-level test for "button disabled"; instead we verify that callerAmount is
    // forwarded correctly and that null callerAmount is handled.

    @Test
    fun `5_1 ConfirmRecurring with variable amount forwards callerAmount null to use case`() = runTest {
        coEvery { confirmRecurring(any(), any(), any(), null) } returns Unit
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = HomeViewModel(getHomeData, confirmRecurring)

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-2", null))
        advanceUntilIdle()

        coVerify(exactly = 1) { confirmRecurring(any(), any(), any(), null) }
    }

    // ---- Scenario 5.3: Variable amount zero rejected by use case ----

    @Test
    fun `5_3 ConfirmRecurring with zero callerAmount propagates ValidationError as ShowError effect`() = runTest {
        val error = DomainException.ValidationError("Amount must be > 0")
        coEvery { confirmRecurring(any(), any(), any(), Money(0L)) } throws error
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = HomeViewModel(getHomeData, confirmRecurring)

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-2", Money(0L)))
        advanceUntilIdle()

        assertTrue(effects.any { it is HomeEffect.ShowError })
        job.cancel()
    }

    // ---- Scenario 6.1: Double confirm same period rejected ----

    @Test
    fun `6_1 ConfirmRecurring already-confirmed period propagates ValidationError as ShowError`() = runTest {
        val error = DomainException.ValidationError("Already confirmed for this period")
        coEvery { confirmRecurring(any(), any(), any(), any()) } throws error
        every { getHomeData(any()) } returns flowOf(emptyHomeData)
        viewModel = HomeViewModel(getHomeData, confirmRecurring)

        val effects = mutableListOf<HomeEffect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onIntent(HomeIntent.ConfirmRecurring("rm-1", Money(1800L)))
        advanceUntilIdle()

        assertTrue(effects.any { it is HomeEffect.ShowError })
        job.cancel()
    }
}
