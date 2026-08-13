package com.emm.justchill.hh.recurring

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.AccountType
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.recurring.CreateRecurringMovementUseCase
import com.emm.domain.recurring.Frequency
import com.emm.domain.recurring.RecurringMovement
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.recurring.UpdateRecurringMovementUseCase
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.error.DomainException
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        createdAt = 0L,
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
    fun `create mode - name change updates state and enables save when account selected and amount set`() = runTest {
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        vm.onIntent(AddEditRecurringMovementIntent.OnNameChange("Netflix"))
        vm.onIntent(AddEditRecurringMovementIntent.OnAccountSelected(testAccount))
        // Amount is required when not variable — empty amount keeps save disabled.
        vm.onIntent(AddEditRecurringMovementIntent.OnAmountChange("1800"))
        advanceUntilIdle()

        assertEquals("Netflix", vm.state.value.name)
        assertTrue(vm.state.value.isSaveEnabled)
    }

    @Test
    fun `create mode - fixed amount empty keeps save disabled even with name and account`() = runTest {
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        vm.onIntent(AddEditRecurringMovementIntent.OnNameChange("Netflix"))
        vm.onIntent(AddEditRecurringMovementIntent.OnAccountSelected(testAccount))
        // amountDigits remains "" — save must be disabled to prevent a null fixed amount.
        advanceUntilIdle()

        assertFalse(vm.state.value.isSaveEnabled)
    }

    @Test
    fun `create mode - fixed amount zero keeps save disabled`() = runTest {
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        vm.onIntent(AddEditRecurringMovementIntent.OnNameChange("Netflix"))
        vm.onIntent(AddEditRecurringMovementIntent.OnAccountSelected(testAccount))
        vm.onIntent(AddEditRecurringMovementIntent.OnAmountChange("0"))
        advanceUntilIdle()

        assertFalse(vm.state.value.isSaveEnabled)
    }

    @Test
    fun `create mode - variable amount toggle enables save without amount digits`() = runTest {
        val vm = createViewModel(id = null)
        advanceUntilIdle()

        vm.onIntent(AddEditRecurringMovementIntent.OnNameChange("Netflix"))
        vm.onIntent(AddEditRecurringMovementIntent.OnAccountSelected(testAccount))
        vm.onIntent(AddEditRecurringMovementIntent.OnVariableAmountToggle(true))
        advanceUntilIdle()

        // Variable amount — no digits required, save should be enabled.
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
        vm.onIntent(AddEditRecurringMovementIntent.OnAmountChange("1800"))
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

    // ---- Account load-ordering (edit mode race) ----

    @Test
    fun `edit mode - account resolved even when accounts flow emits after template load`() = runTest {
        // Simulate the race: accounts flow is a MutableSharedFlow that starts with no emission.
        // The template loads (via recurringRepository.find) before accounts arrive.
        val accountsFlow = MutableSharedFlow<List<Account>>(replay = 1)
        every { accountRepository.all() } returns accountsFlow

        coEvery { recurringRepository.find(RecurringMovementId("rm-1")) } returns testTemplate
        val vm = createViewModel(id = "rm-1")

        // Let the init block run — accounts have NOT yet been emitted.
        advanceUntilIdle()

        // At this point template is loaded, but accounts not yet; selectedAccount may be null.
        // pendingAccountId should carry the unresolved id.
        assertNull(vm.state.value.selectedAccount)

        // Now emit accounts — the combine collector should resolve pendingAccountId.
        accountsFlow.emit(listOf(testAccount))
        advanceUntilIdle()

        assertNotNull(vm.state.value.selectedAccount)
        assertEquals("acc-1", vm.state.value.selectedAccount?.accountId?.value)
        // pendingAccountId cleared after resolution.
        assertNull(vm.state.value.pendingAccountId)
    }

    // ---- Category load-ordering (edit mode race) ----

    @Test
    fun `edit mode - category resolved even when categories flow emits after template load`() = runTest {
        // Simulate the race: categories flow has not emitted when the template loads.
        // Without a pendingCategoryId safety net the category silently resolves to null,
        // which would WIPE the category on save.
        val categoriesFlow = MutableSharedFlow<List<Category>>(replay = 1)
        every { categoryRepository.all() } returns categoriesFlow

        val templateWithCategory = testTemplate.copy(categoryId = CategoryId("cat-1"))
        coEvery { recurringRepository.find(RecurringMovementId("rm-1")) } returns templateWithCategory
        val vm = createViewModel(id = "rm-1")

        // Template loaded, categories NOT yet emitted — category unresolved.
        advanceUntilIdle()
        assertNull(vm.state.value.selectedCategory)

        // Now emit categories — the combine collector must resolve pendingCategoryId.
        categoriesFlow.emit(
            listOf(Category(CategoryId("cat-1"), "Bar", "bar", "purple", CategoryType.Spend)),
        )
        advanceUntilIdle()

        assertNotNull(vm.state.value.selectedCategory)
        assertEquals("cat-1", vm.state.value.selectedCategory?.categoryId?.value)
        assertNull(vm.state.value.pendingCategoryId)
    }

    @Test
    fun `edit mode - account resolved when accounts flow emits before template load`() = runTest {
        // Happy path: accounts arrive first (typical with SQLDelight immediate emission).
        coEvery { recurringRepository.find(RecurringMovementId("rm-1")) } returns testTemplate
        // accountRepository.all() returns flowOf(listOf(testAccount)) via setUp()
        val vm = createViewModel(id = "rm-1")
        advanceUntilIdle()

        assertNotNull(vm.state.value.selectedAccount)
        assertEquals("acc-1", vm.state.value.selectedAccount?.accountId?.value)
        assertNull(vm.state.value.pendingAccountId)
    }

    // ---- The category belongs to the type ----

    private val spendCategory = Category(CategoryId("cat-spend"), "Bar", "bar", "purple", CategoryType.Spend)
    private val incomeCategory = Category(CategoryId("cat-income"), "Sueldo", "salary", "green", CategoryType.Income)

    /**
     * A template mints a transaction of its own type every month, and `(categoryId, type)` is a
     * foreign key since schema v5 — so a template holding a category of the other type is not one
     * bad row, it is a generator of rows the database refuses. The form used to offer every
     * category regardless of the type selected.
     */
    @Test
    fun `only the categories of the selected type are offered`() = runTest {
        every { categoryRepository.all() } returns flowOf(listOf(spendCategory, incomeCategory))

        val vm = createViewModel()
        advanceUntilIdle()

        // The form opens on Spend.
        assertEquals(TransactionType.Spend, vm.state.value.type)
        assertEquals(listOf("cat-spend"), vm.state.value.categories.map { it.categoryId.value })
    }

    @Test
    fun `switching the type re-cuts the list and drops a selection that no longer fits`() = runTest {
        every { categoryRepository.all() } returns flowOf(listOf(spendCategory, incomeCategory))
        val vm = createViewModel()
        advanceUntilIdle()
        vm.onIntent(AddEditRecurringMovementIntent.OnCategorySelected(vm.state.value.categories.single()))
        advanceUntilIdle()
        assertNotNull(vm.state.value.selectedCategory)

        vm.onIntent(AddEditRecurringMovementIntent.OnTypeChange(TransactionType.Income))
        advanceUntilIdle()

        // Keeping the Spend category on an Income template is exactly the pair the schema refuses,
        // and it would only surface at save time as a generic database error.
        assertNull(vm.state.value.selectedCategory)
        assertEquals(listOf("cat-income"), vm.state.value.categories.map { it.categoryId.value })
    }

    @Test
    fun `edit mode - an Income template is offered Income categories`() = runTest {
        every { categoryRepository.all() } returns flowOf(listOf(spendCategory, incomeCategory))
        val incomeTemplate = testTemplate.copy(type = TransactionType.Income, categoryId = CategoryId("cat-income"))
        coEvery { recurringRepository.find(RecurringMovementId("rm-1")) } returns incomeTemplate

        val vm = createViewModel(id = "rm-1")
        advanceUntilIdle()

        assertEquals("cat-income", vm.state.value.selectedCategory?.categoryId?.value)
        assertEquals(listOf("cat-income"), vm.state.value.categories.map { it.categoryId.value })
    }
}
