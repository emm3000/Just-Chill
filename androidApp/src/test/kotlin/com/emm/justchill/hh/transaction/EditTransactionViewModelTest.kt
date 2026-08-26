package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.Transaction
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionUpdate
import com.emm.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.time.FakeTodayFlow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class EditTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val today = LocalDate(2026, Month.AUGUST, 10)
    private val todayDates = MutableStateFlow(today)

    /**
     * Deliberately FIRST in the catalog, and never what the stored movement points at or what any
     * test picks. `accountSelected` and `categorySelected` are plain lookups with no fallback, so
     * a fixture of one answers correctly even with the lookup deleted — every assertion about a
     * selection would then pass while pinning nothing.
     */
    private val decoyAccount = Account(AccountId("interbank"), "Interbank")

    private val account = Account(AccountId("bcp"), "BCP")

    /** Neither stored nor first: only a live pick can put this one on screen or in a write. */
    private val pickedAccount = Account(AccountId("yape"), "Yape")

    private val accounts = listOf(decoyAccount, account, pickedAccount)

    /** The decoy's category half — same reasoning, and never what the stored movement points at. */
    private val decoyCategory = Category(
        categoryId = CategoryId("transport"),
        name = "Transporte",
        icon = "bus",
        color = "yellow",
        categoryType = CategoryType.Spend,
    )

    private val category = Category(
        categoryId = CategoryId("food"),
        name = "Comida",
        icon = "food",
        color = "blue",
        categoryType = CategoryType.Spend,
    )

    private val categories = listOf(decoyCategory, category)

    /** Recorded on 4 March 2026 at 09:15 — a day that is neither today nor yesterday. */
    private val marchDay = LocalDate(2026, Month.MARCH, 4)
    private val marchOccurredAt = LocalDateTime(marchDay, LocalTime(9, 15, 33))

    private val storedTransaction = Transaction(
        transactionId = TransactionId("tx-1"),
        type = TransactionType.Spend,
        amount = Money(8540),
        description = "Mercado",
        occurredAt = marchOccurredAt,
        accountId = account.accountId,
        categoryId = category.categoryId,
    )

    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(accounts)
    }
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(categories)
    }
    private val transactionRepository = mockk<TransactionRepository>()
    private val updateTransaction = mockk<UpdateTransactionUseCase>(relaxed = true)
    private val deleteTransaction = mockk<DeleteTransactionUseCase>(relaxed = true)
    private val getTopUsedCategoryIds = mockk<GetTopUsedCategoryIdsUseCase>()

    @Before
    fun setupDefaults() {
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns storedTransaction
        coEvery { getTopUsedCategoryIds.invoke(any(), any(), any()) } returns emptyList()
    }

    private fun buildViewModel(): EditTransactionViewModel = EditTransactionViewModel(
        transactionId = "tx-1",
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        updateTransaction = updateTransaction,
        transactionRepository = transactionRepository,
        deleteTransaction = deleteTransaction,
        getTopUsedCategoryIds = getTopUsedCategoryIds,
        todayFlow = FakeTodayFlow(todayDates),
    )

    @Test
    fun `state carries the transaction's own day after load`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(marchDay, vm.state.value.date)
    }

    @Test
    fun `the date label is the transaction's day, not today`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("4 mar", vm.state.value.dateLabel)
    }

    @Test
    fun `today comes from the injected TodayFlow`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(today, vm.state.value.today)
    }

    @Test
    fun `today catches up on the next interaction after midnight`() = runTest(testDispatcher) {
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns
            storedTransaction.copy(occurredAt = LocalDateTime(today, LocalTime(9, 15)))

        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals("Hoy", vm.state.value.dateLabel)

        todayDates.value = LocalDate(2026, Month.AUGUST, 11)
        vm.onIntent(EditTransactionIntent.OnAmountChange("9000"))
        advanceUntilIdle()

        assertEquals(today, vm.state.value.date)
        assertEquals("Ayer", vm.state.value.dateLabel)
    }

    @Test
    fun `an evening transaction keeps its own day`() = runTest(testDispatcher) {
        val evening = storedTransaction.copy(occurredAt = LocalDateTime(marchDay, LocalTime(23, 30)))
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns evening

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(marchDay, vm.state.value.date)
    }

    @Test
    fun `OnDateSelected replaces the day in the state`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnDateSelected(LocalDate(2026, Month.JUNE, 13)))
        advanceUntilIdle()

        assertEquals(LocalDate(2026, Month.JUNE, 13), vm.state.value.date)
        assertEquals("13 jun", vm.state.value.dateLabel)
    }

    @Test
    fun `picking a different day enables save`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnDateSelected(LocalDate(2026, Month.JUNE, 13)))
        advanceUntilIdle()

        assertEquals(true, vm.state.value.isEnabled)
    }

    @Test
    fun `re-picking the day already loaded leaves save disabled`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnDateSelected(marchDay))
        advanceUntilIdle()

        assertEquals(false, vm.state.value.isEnabled)
    }

    @Test
    fun `moving the transaction to another day carries its recorded hour across`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        val newDay = LocalDate(2026, Month.JUNE, 13)
        vm.onIntent(EditTransactionIntent.OnDateSelected(newDay))
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnSave)
        advanceUntilIdle()

        val update = slot<TransactionUpdate>()
        coVerify { updateTransaction.invoke(storedTransaction, capture(update)) }
        assertEquals(LocalDateTime(newDay, LocalTime(9, 15, 33)), update.captured.occurredAt)
    }

    @Test
    fun `an edit that does not touch the date sends back the stored value, byte for byte`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnAmountChange("9000"))
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnSave)
        advanceUntilIdle()

        val update = slot<TransactionUpdate>()
        coVerify { updateTransaction.invoke(storedTransaction, capture(update)) }
        assertEquals(storedTransaction.occurredAt, update.captured.occurredAt)
    }

    @Test
    fun `the loaded movement's own account is selected, not the catalog's first`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(account.accountId, vm.state.value.accountSelected?.accountId)
        assertFalse(vm.state.value.isEnabled, "resolving the stored account is not an edit")
    }

    @Test
    fun `the account the user picks is the account the update carries`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        // Neither stored nor first in the catalog, so nothing but the id lookup can answer with it.
        vm.onIntent(EditTransactionIntent.OnAccountSelected(pickedAccount))
        advanceUntilIdle()
        assertEquals(pickedAccount.accountId, vm.state.value.accountSelected?.accountId)

        vm.onIntent(EditTransactionIntent.OnSave)
        advanceUntilIdle()

        val update = slot<TransactionUpdate>()
        coVerify { updateTransaction.invoke(storedTransaction, capture(update)) }
        assertEquals(pickedAccount.accountId, update.captured.accountId)
    }

    @Test
    fun `a picked category of the current type wins over the stored one`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(category.categoryId, vm.state.value.categorySelected?.categoryId, "stored, before any pick")

        val picked = vm.state.value.categories.single { it.categoryId == decoyCategory.categoryId }
        vm.onIntent(EditTransactionIntent.OnCategorySelected(picked))
        advanceUntilIdle()

        // The stored category is still on offer, so only the live pick can produce this answer.
        assertEquals(decoyCategory.categoryId, vm.state.value.categorySelected?.categoryId)

        vm.onIntent(EditTransactionIntent.OnSave)
        advanceUntilIdle()

        val update = slot<TransactionUpdate>()
        coVerify { updateTransaction.invoke(storedTransaction, capture(update)) }
        assertEquals(decoyCategory.categoryId, update.captured.categoryId)
    }

    @Test
    fun `switching type away and back re-offers the movement's own category`() = runTest(testDispatcher) {
        val incomeCategory = Category(
            categoryId = CategoryId("salary"),
            name = "Sueldo",
            icon = "money",
            color = "green",
            categoryType = CategoryType.Income,
        )
        every { categoryRepository.all() } returns flowOf(categories + incomeCategory)

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnTransactionTypeChange(TransactionType.Income))
        advanceUntilIdle()
        vm.onIntent(EditTransactionIntent.OnCategorySelected(vm.state.value.categories.single()))
        advanceUntilIdle()
        assertEquals(incomeCategory.categoryId, vm.state.value.categorySelected?.categoryId)

        vm.onIntent(EditTransactionIntent.OnTransactionTypeChange(TransactionType.Spend))
        advanceUntilIdle()

        // The live pick is an Income id the Spend list cannot resolve: only the stored-id tier
        // can answer here, and answering null would be a silent uncategorize on the next save.
        assertEquals(category.categoryId, vm.state.value.categorySelected?.categoryId)
    }

    @Test
    fun `an uncategorized movement does not acquire a category on load`() = runTest(testDispatcher) {
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns
            storedTransaction.copy(categoryId = null)

        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.categorySelected, "nobody picked this")
        assertFalse(vm.state.value.isEnabled, "opening a screen is not an edit")
    }

    @Test
    fun `editing an uncategorized movement saves it still uncategorized`() = runTest(testDispatcher) {
        val uncategorized = storedTransaction.copy(categoryId = null)
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns uncategorized
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnAmountChange("9000"))
        advanceUntilIdle()
        vm.onIntent(EditTransactionIntent.OnSave)
        advanceUntilIdle()

        val update = slot<TransactionUpdate>()
        coVerify { updateTransaction.invoke(uncategorized, capture(update)) }
        assertNull(update.captured.categoryId, "an amount edit must not write a category")
    }

    @Test
    fun `switching the type of an uncategorized movement still leaves it uncategorized`() = runTest(testDispatcher) {
        // The Income category has to exist for this to be able to fail: with an empty list for
        // the new type, `firstOrNull()` is null for the wrong reason.
        val incomeCategory = Category(
            categoryId = CategoryId("salary"),
            name = "Sueldo",
            icon = "money",
            color = "green",
            categoryType = CategoryType.Income,
        )
        every { categoryRepository.all() } returns flowOf(listOf(category, incomeCategory))
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns
            storedTransaction.copy(categoryId = null)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnTransactionTypeChange(TransactionType.Income))
        advanceUntilIdle()

        assertEquals(1, vm.state.value.categories.size, "the Income category must be on offer")
        assertNull(vm.state.value.categorySelected, "on offer, but not chosen for the user")
    }

    @Test
    fun `a category deleted while the form is open stops being selected`() = runTest(testDispatcher) {
        val categories = MutableSharedFlow<List<Category>>(replay = 1)
        every { categoryRepository.all() } returns categories
        categories.emit(listOf(category))

        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(category.categoryId, vm.state.value.categorySelected?.categoryId)

        categories.emit(emptyList())
        advanceUntilIdle()

        assertNull(vm.state.value.categorySelected, "the row the selection pointed at is gone")
    }

    @Test
    fun `a stored category the catalog can no longer offer does not arm save at rest`() = runTest(testDispatcher) {
        // DeleteCategoryUseCase soft-deletes unconditionally and categories.sq:all filters
        // `deletedAt IS NULL`, so the row keeps a categoryId nothing in the catalog resolves.
        every { categoryRepository.all() } returns flowOf(emptyList())

        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.categorySelected, "the row it pointed at is gone")
        assertFalse(vm.state.value.isEnabled, "opening a screen is not an edit the user made")
    }

    @Test
    fun `deleting targets the route's transaction even when the row never loaded`() = runTest(testDispatcher) {
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns null

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(EditTransactionIntent.OnDelete)
        advanceUntilIdle()

        coVerify { deleteTransaction.invoke(TransactionId("tx-1")) }
    }

    @Test
    fun `switching the type does not adopt a category of the new type`() = runTest(testDispatcher) {
        val incomeCategory = Category(
            categoryId = CategoryId("salary"),
            name = "Sueldo",
            icon = "money",
            color = "green",
            categoryType = CategoryType.Income,
        )
        every { categoryRepository.all() } returns flowOf(listOf(category, incomeCategory))
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnTransactionTypeChange(TransactionType.Income))
        advanceUntilIdle()

        assertEquals(1, vm.state.value.categories.size, "the Income category must be on offer")
        assertNull(vm.state.value.categorySelected, "the stored category belongs to the old type")
    }
}
