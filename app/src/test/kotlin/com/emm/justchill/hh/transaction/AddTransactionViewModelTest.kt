package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.CreateTransactionUseCase
import com.emm.domain.transaction.FrequentCombo
import com.emm.domain.transaction.GetFrequentCombosUseCase
import com.emm.domain.transaction.GetLastUsedAccountIdUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
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

class AddTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val account1 = Account(AccountId("yape"), "Yape")
    private val account2 = Account(AccountId("bcp"), "BCP")

    private val category1 = Category(
        categoryId = CategoryId("food"),
        name = "Comida",
        icon = "food",
        color = "blue",
        categoryType = CategoryType.Spend,
    )
    private val category2 = Category(
        categoryId = CategoryId("salary"),
        name = "Sueldo",
        icon = "money",
        color = "green",
        categoryType = CategoryType.Income,
    )

    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(listOf(account1, account2))
    }

    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(listOf(category1, category2))
    }

    private val createTransaction = mockk<CreateTransactionUseCase>(relaxed = true)
    private val getTopUsedCategoryIds = mockk<GetTopUsedCategoryIdsUseCase>()
    private val getFrequentCombos = mockk<GetFrequentCombosUseCase>()
    private val getLastUsedAccountId = mockk<GetLastUsedAccountIdUseCase>()

    @Before
    fun setupDefaults() {
        coEvery { getTopUsedCategoryIds.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns emptyList()
        coEvery { getFrequentCombos.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns emptyList()
        coEvery { getLastUsedAccountId.invoke() } returns null
    }

    private fun buildViewModel(): AddTransactionViewModel = AddTransactionViewModel(
        createTransaction = createTransaction,
        getTopUsedCategoryIds = getTopUsedCategoryIds,
        getFrequentCombos = getFrequentCombos,
        getLastUsedAccountId = getLastUsedAccountId,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
    )

    // ── frequentCombos state ──────────────────────────────────────────────────

    @Test
    fun `frequentCombos populated on init when combos match accounts and categories`() = runTest(testDispatcher) {
        // Default transactionType is Income
        val combo = FrequentCombo(AccountId("bcp"), CategoryId("salary"), TransactionType.Income)
        coEvery { getFrequentCombos.invoke(TransactionType.Income, any<Int>(), any<Int>()) } returns listOf(combo)

        val vm = buildViewModel()
        advanceUntilIdle()

        val combos = vm.state.value.frequentCombos
        assertEquals(1, combos.size)
        assertEquals("BCP · Sueldo", combos[0].label)
        assertEquals("bcp", combos[0].accountId)
        assertEquals("salary", combos[0].categoryId)
    }

    @Test
    fun `frequentCombos is empty on init when history is absent`() = runTest(testDispatcher) {
        // default stubbing from setupDefaults returns emptyList
        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.frequentCombos.isEmpty())
    }

    @Test
    fun `frequentCombos reloads on OnTransactionTypeChange`() = runTest(testDispatcher) {
        val spendCombo = FrequentCombo(AccountId("yape"), CategoryId("food"), TransactionType.Spend)
        val incomeCombo = FrequentCombo(AccountId("bcp"), CategoryId("salary"), TransactionType.Income)
        coEvery { getFrequentCombos.invoke(TransactionType.Spend, any<Int>(), any<Int>()) } returns listOf(spendCombo)
        coEvery { getFrequentCombos.invoke(TransactionType.Income, any<Int>(), any<Int>()) } returns listOf(incomeCombo)

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Spend))
        advanceUntilIdle()

        val combos = vm.state.value.frequentCombos
        assertEquals(1, combos.size)
        assertEquals("Yape · Comida", combos[0].label)
        coVerify(atLeast = 1) { getFrequentCombos.invoke(TransactionType.Spend, any<Int>(), any<Int>()) }
    }

    @Test
    fun `combo whose account was deleted is excluded from frequentCombos`() = runTest(testDispatcher) {
        val orphanCombo = FrequentCombo(AccountId("deleted-acc"), CategoryId("salary"), TransactionType.Income)
        coEvery { getFrequentCombos.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns listOf(orphanCombo)

        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.frequentCombos.isEmpty())
    }

    @Test
    fun `combo whose category was deleted is excluded from frequentCombos`() = runTest(testDispatcher) {
        val orphanCombo = FrequentCombo(AccountId("bcp"), CategoryId("deleted-cat"), TransactionType.Income)
        coEvery { getFrequentCombos.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns listOf(orphanCombo)

        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.frequentCombos.isEmpty())
    }

    // ── OnFrequentComboSelected intent ────────────────────────────────────────

    @Test
    fun `OnFrequentComboSelected sets accountSelected and categorySelected`() = runTest(testDispatcher) {
        // Default type is Income — use an Income combo (bcp + salary)
        val combo = FrequentCombo(AccountId("bcp"), CategoryId("salary"), TransactionType.Income)
        coEvery { getFrequentCombos.invoke(TransactionType.Income, any<Int>(), any<Int>()) } returns listOf(combo)

        val vm = buildViewModel()
        advanceUntilIdle()

        val comboUi = vm.state.value.frequentCombos.first()
        vm.onIntent(AddTransactionIntent.OnFrequentComboSelected(comboUi))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("bcp", state.accountSelected?.accountId?.value)
        assertEquals("salary", state.categorySelected?.categoryId?.value)
    }

    @Test
    fun `OnFrequentComboSelected does not change transactionType`() = runTest(testDispatcher) {
        val combo = FrequentCombo(AccountId("bcp"), CategoryId("salary"), TransactionType.Income)
        coEvery { getFrequentCombos.invoke(TransactionType.Income, any<Int>(), any<Int>()) } returns listOf(combo)

        val vm = buildViewModel()
        advanceUntilIdle()

        val initialType = vm.state.value.transactionType
        val comboUi = vm.state.value.frequentCombos.first()
        vm.onIntent(AddTransactionIntent.OnFrequentComboSelected(comboUi))
        advanceUntilIdle()

        assertEquals(initialType, vm.state.value.transactionType)
    }

    @Test
    fun `FocusAmountField effect is emitted when OnFrequentComboSelected is handled`() = runTest(testDispatcher) {
        val combo = FrequentCombo(AccountId("bcp"), CategoryId("salary"), TransactionType.Income)
        coEvery { getFrequentCombos.invoke(TransactionType.Income, any<Int>(), any<Int>()) } returns listOf(combo)

        val vm = buildViewModel()
        advanceUntilIdle()

        val effects = mutableListOf<AddTransactionEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        val comboUi = vm.state.value.frequentCombos.first()
        vm.onIntent(AddTransactionIntent.OnFrequentComboSelected(comboUi))
        advanceUntilIdle()

        job.cancel()
        val focusEffects = effects.filterIsInstance<AddTransactionEffect.FocusAmountField>()
        assertEquals(1, focusEffects.size)
    }

    // ── Last-used account pre-selection ───────────────────────────────────────

    @Test
    fun `accountSelected is last-used account on init when history exists`() = runTest(testDispatcher) {
        coEvery { getLastUsedAccountId.invoke() } returns AccountId("bcp")

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("bcp", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `accountSelected falls back to firstOrNull when no history`() = runTest(testDispatcher) {
        // default stub returns null

        val vm = buildViewModel()
        advanceUntilIdle()

        // first in list is account1 (yape)
        assertEquals("yape", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `accountSelected falls back to firstOrNull when last-used account was deleted`() = runTest(testDispatcher) {
        coEvery { getLastUsedAccountId.invoke() } returns AccountId("deleted-account")

        val vm = buildViewModel()
        advanceUntilIdle()

        // "deleted-account" is not in the loaded list — falls back to first
        assertEquals("yape", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `OnReset restores last-used account pre-selection`() = runTest(testDispatcher) {
        coEvery { getLastUsedAccountId.invoke() } returns AccountId("bcp")

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnAccountSelected(account1)) // switch to yape
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnReset)
        advanceUntilIdle()

        // After reset, accountSelected should again be the cached last-used (bcp)
        assertEquals("bcp", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `accountSelected is null when account list is empty`() = runTest(testDispatcher) {
        every { accountRepository.all() } returns flowOf(emptyList())

        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.accountSelected)
    }
}
