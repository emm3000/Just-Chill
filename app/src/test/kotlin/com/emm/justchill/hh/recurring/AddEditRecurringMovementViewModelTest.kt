package com.emm.justchill.hh.recurring

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountType
import com.emm.domain.category.CategoryRepository
import com.emm.domain.recurring.CreateRecurringMovementUseCase
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.recurring.UpdateRecurringMovementUseCase
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

class AddEditRecurringMovementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val accountRepository = mockk<AccountRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val recurringRepository = mockk<RecurringMovementRepository>()
    private val createRecurring = mockk<CreateRecurringMovementUseCase>()
    private val updateRecurring = mockk<UpdateRecurringMovementUseCase>()

    private val testAccount = Account(accountId = AccountId("acc-1"), name = "BCP", type = AccountType.Bank)

    private val testTemplate = RecurringMovement(
        id = RecurringMovementId("rm-1"),
        name = "Netflix",
        type = TransactionType.Spend,
        amount = Money(1800L),
        description = "Streaming",
        categoryId = null,
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = 15,
        isActive = true,
        lastConfirmedPeriod = null,
    )

    @Before
    fun setUp() {
        every { accountRepository.all() } returns flowOf(listOf(testAccount))
        every { categoryRepository.all() } returns flowOf(emptyList())
    }

    private fun createViewModel(id: String? = null) = AddEditRecurringMovementViewModel(
        id = id,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        recurringRepository = recurringRepository,
        createRecurring = createRecurring,
        updateRecurring = updateRecurring,
    )

    @Test
    fun `create mode - initial state has isEdit false and empty fields`() = runTest {
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        assertFalse(vm.state.value.isEdit)
        assertEquals("", vm.state.value.name)
    }

    @Test
    fun `create mode - name change updates state and enables save when account selected`() = runTest {
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        vm.onIntent(AddEditRecurringMovementIntent.OnNameChange("Netflix"))
        vm.onIntent(AddEditRecurringMovementIntent.OnAccountSelected(testAccount))
        advanceUntilIdle()

        assertEquals("Netflix", vm.state.value.name)
        assertTrue(vm.state.value.isSaveEnabled)
    }

    @Test
    fun `create mode - empty name disables save`() = runTest {
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        vm.onIntent(AddEditRecurringMovementIntent.OnAccountSelected(testAccount))
        vm.onIntent(AddEditRecurringMovementIntent.OnNameChange("  "))
        advanceUntilIdle()

        assertFalse(vm.state.value.isSaveEnabled)
    }

    @Test
    fun `create mode - save calls createRecurring and emits NavigateBack`() = runTest {
        coEvery { createRecurring(any()) } returns Unit
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        val effects = mutableListOf<AddEditRecurringMovementEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AddEditRecurringMovementIntent.OnNameChange("Netflix"))
        vm.onIntent(AddEditRecurringMovementIntent.OnAccountSelected(testAccount))
        vm.onIntent(AddEditRecurringMovementIntent.Save)
        advanceUntilIdle()

        coVerify(exactly = 1) { createRecurring(any()) }
        assertTrue(effects.any { it is AddEditRecurringMovementEffect.NavigateBack })
        job.cancel()
    }

    @Test
    fun `edit mode - loads template and pre-populates fields`() = runTest {
        coEvery { recurringRepository.find(RecurringMovementId("rm-1")) } returns testTemplate
        val vm = createViewModel(id = "rm-1")
        advanceUntilIdle()

        assertTrue(vm.state.value.isEdit)
        assertEquals("Netflix", vm.state.value.name)
        assertEquals(15, vm.state.value.dayOfMonth)
        assertEquals(TransactionType.Spend, vm.state.value.type)
    }

    @Test
    fun `edit mode - save calls updateRecurring and emits NavigateBack`() = runTest {
        coEvery { recurringRepository.find(RecurringMovementId("rm-1")) } returns testTemplate
        coEvery { updateRecurring(any(), any()) } returns Unit
        val vm = createViewModel(id = "rm-1")
        advanceUntilIdle()

        val effects = mutableListOf<AddEditRecurringMovementEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AddEditRecurringMovementIntent.Save)
        advanceUntilIdle()

        coVerify(exactly = 1) { updateRecurring(RecurringMovementId("rm-1"), any()) }
        assertTrue(effects.any { it is AddEditRecurringMovementEffect.NavigateBack })
        job.cancel()
    }

    @Test
    fun `save with ValidationError emits ShowError effect`() = runTest {
        coEvery { createRecurring(any()) } throws DomainException.ValidationError("Name required")
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        val effects = mutableListOf<AddEditRecurringMovementEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(AddEditRecurringMovementIntent.OnNameChange("x"))
        vm.onIntent(AddEditRecurringMovementIntent.OnAccountSelected(testAccount))
        vm.onIntent(AddEditRecurringMovementIntent.Save)
        advanceUntilIdle()

        assertTrue(effects.any { it is AddEditRecurringMovementEffect.ShowError })
        job.cancel()
    }

    @Test
    fun `variable amount toggle sets isVariableAmount in state`() = runTest {
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        vm.onIntent(AddEditRecurringMovementIntent.OnVariableAmountToggle(true))
        advanceUntilIdle()

        assertTrue(vm.state.value.isVariableAmount)
    }
}
