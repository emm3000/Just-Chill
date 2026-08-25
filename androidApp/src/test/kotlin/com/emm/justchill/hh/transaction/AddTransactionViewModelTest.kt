package com.emm.justchill.hh.transaction

import androidx.lifecycle.ViewModelStore
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
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.TransactionInsert
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class AddTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 10)
    private val tomorrow = LocalDate(2026, Month.AUGUST, 11)

    private class MovableClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private fun instantAt(date: LocalDate, hour: Int, minute: Int): Instant = Instant.fromEpochMilliseconds(
        LocalDateTime(date, LocalTime(hour, minute)).toInstant(lima).toEpochMilliseconds(),
    )

    private val fixedClock = MovableClock(instantAt(today, hour = 14, minute = 30))

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

    // Ordered before category2 so "the first Income category" (the ordinary default a type switch
    // picks) is never "salary" — the id the resolved-preselect tests below use — keeping a test that
    // asserts on the default unambiguous from one that asserts on a resolved match.
    private val category3 = Category(
        categoryId = CategoryId("bonus"),
        name = "Bono",
        icon = "gift",
        color = "purple",
        categoryType = CategoryType.Income,
    )

    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(listOf(account1, account2), listOf(account1, account2))
    }

    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(
            listOf(category1, category3, category2),
            listOf(category1, category3, category2),
        )
    }

    private val createTransaction = mockk<CreateTransactionUseCase>(relaxed = true)
    private val getTopUsedCategoryIds = mockk<GetTopUsedCategoryIdsUseCase>()
    private val getFrequentCombos = mockk<GetFrequentCombosUseCase>()
    private val transactionStatsRepository = mockk<TransactionStatsRepository>()

    @Before
    fun setupDefaults() {
        coEvery { getTopUsedCategoryIds.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns emptyList()
        coEvery { getFrequentCombos.invoke(any<TransactionType>(), any<Int>(), any<Int>()) } returns emptyList()
        coEvery { transactionStatsRepository.lastUsedAccountId() } returns null
    }

    private fun buildViewModel(): AddTransactionViewModel = AddTransactionViewModel(
        createTransaction = createTransaction,
        getTopUsedCategoryIds = getTopUsedCategoryIds,
        getFrequentCombos = getFrequentCombos,
        transactionStatsRepository = transactionStatsRepository,
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        clock = fixedClock,
        zone = lima,
    )

    @Test
    fun `date starts unset and reads as Hoy`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.date)
        assertEquals(today, vm.state.value.today)
        assertEquals("Hoy", vm.state.value.dateLabel)
    }

    @Test
    fun `initial state defaults transactionType to Spend`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(TransactionType.Spend, vm.state.value.transactionType)
    }

    @Test
    fun `an untouched date saves as the day it is saved on, not the day the screen opened`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        fixedClock.instant = instantAt(tomorrow, hour = 0, minute = 5)

        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        advanceUntilIdle()
        vm.onIntent(AddTransactionIntent.OnSave)
        advanceUntilIdle()

        val insert = slot<TransactionInsert>()
        coVerify { createTransaction.invoke(capture(insert)) }
        assertEquals(LocalDateTime(tomorrow, LocalTime(0, 5)), insert.captured.occurredAt)
    }

    @Test
    fun `a picked date is not re-resolved when the clock rolls over`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        val picked = LocalDate(2026, Month.JUNE, 13)
        vm.onIntent(AddTransactionIntent.OnDateSelected(picked))
        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        advanceUntilIdle()

        fixedClock.instant = instantAt(tomorrow, hour = 0, minute = 5)

        vm.onIntent(AddTransactionIntent.OnSave)
        advanceUntilIdle()

        val insert = slot<TransactionInsert>()
        coVerify { createTransaction.invoke(capture(insert)) }
        assertEquals(LocalDateTime(picked, LocalTime(0, 5)), insert.captured.occurredAt)
    }

    @Test
    fun `today catches up on the next interaction after midnight`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(today, vm.state.value.today)

        fixedClock.instant = instantAt(tomorrow, hour = 0, minute = 5)
        vm.onIntent(AddTransactionIntent.OnAmountChange("1"))
        advanceUntilIdle()

        assertEquals(tomorrow, vm.state.value.today)
    }

    @Test
    fun `a date picked yesterday reads as Ayer once the day rolls over`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnDateSelected(today))
        advanceUntilIdle()
        assertEquals("Hoy", vm.state.value.dateLabel)

        fixedClock.instant = instantAt(tomorrow, hour = 0, minute = 5)
        vm.onIntent(AddTransactionIntent.OnAmountChange("1"))
        advanceUntilIdle()

        assertEquals("Ayer", vm.state.value.dateLabel)
    }

    @Test
    fun `OnDateSelected replaces the day in the state`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnDateSelected(LocalDate(2026, Month.JUNE, 13)))
        advanceUntilIdle()

        assertEquals(LocalDate(2026, Month.JUNE, 13), vm.state.value.date)
        assertEquals("13 jun", vm.state.value.dateLabel)
    }

    @Test
    fun `save sends the picked day with the hour it was recorded at`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        val picked = LocalDate(2026, Month.JUNE, 13)
        vm.onIntent(AddTransactionIntent.OnDateSelected(picked))
        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnSave)
        advanceUntilIdle()

        val insert = slot<TransactionInsert>()
        coVerify { createTransaction.invoke(capture(insert)) }
        assertEquals(LocalDateTime(picked, LocalTime(14, 30)), insert.captured.occurredAt)
    }

    @Test
    fun `OnReset puts the date back to unset`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnDateSelected(LocalDate(2026, Month.JUNE, 13)))
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnReset)
        advanceUntilIdle()

        assertNull(vm.state.value.date)
        assertEquals("Hoy", vm.state.value.dateLabel)
    }

    @Test
    fun `OnReset restores transactionType to the Spend default`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Income))
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnReset)
        advanceUntilIdle()

        assertEquals(TransactionType.Spend, vm.state.value.transactionType)
    }

    @Test
    fun `frequentCombos populated on init when combos match accounts and categories`() = runTest(testDispatcher) {
        val combo = FrequentCombo(AccountId("yape"), CategoryId("food"), TransactionType.Spend)
        coEvery { getFrequentCombos.invoke(TransactionType.Spend, any<Int>(), any<Int>()) } returns listOf(combo)

        val vm = buildViewModel()
        advanceUntilIdle()

        val combos = vm.state.value.frequentCombos
        assertEquals(1, combos.size)
        assertEquals("Yape · Comida", combos[0].label)
        assertEquals("yape", combos[0].accountId)
        assertEquals("food", combos[0].categoryId)
    }

    @Test
    fun `frequentCombos is empty on init when history is absent`() = runTest(testDispatcher) {
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

        vm.onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Income))
        advanceUntilIdle()

        val combos = vm.state.value.frequentCombos
        assertEquals(1, combos.size)
        assertEquals("BCP · Sueldo", combos[0].label)
        coVerify(atLeast = 1) { getFrequentCombos.invoke(TransactionType.Income, any<Int>(), any<Int>()) }
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

    @Test
    fun `OnFrequentComboSelected sets accountSelected and categorySelected`() = runTest(testDispatcher) {
        val combo = FrequentCombo(AccountId("yape"), CategoryId("food"), TransactionType.Spend)
        coEvery { getFrequentCombos.invoke(TransactionType.Spend, any<Int>(), any<Int>()) } returns listOf(combo)

        val vm = buildViewModel()
        advanceUntilIdle()

        val comboUi = vm.state.value.frequentCombos.first()
        vm.onIntent(AddTransactionIntent.OnFrequentComboSelected(comboUi))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("yape", state.accountSelected?.accountId?.value)
        assertEquals("food", state.categorySelected?.categoryId?.value)
    }

    @Test
    fun `OnFrequentComboSelected does not change transactionType`() = runTest(testDispatcher) {
        val combo = FrequentCombo(AccountId("yape"), CategoryId("food"), TransactionType.Spend)
        coEvery { getFrequentCombos.invoke(TransactionType.Spend, any<Int>(), any<Int>()) } returns listOf(combo)

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
        val combo = FrequentCombo(AccountId("yape"), CategoryId("food"), TransactionType.Spend)
        coEvery { getFrequentCombos.invoke(TransactionType.Spend, any<Int>(), any<Int>()) } returns listOf(combo)

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

    @Test
    fun `accountSelected is last-used account on init when history exists`() = runTest(testDispatcher) {
        coEvery { transactionStatsRepository.lastUsedAccountId() } returns AccountId("bcp")

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("bcp", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `accountSelected falls back to firstOrNull when no history`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("yape", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `accountSelected falls back to firstOrNull when last-used account was deleted`() = runTest(testDispatcher) {
        coEvery { transactionStatsRepository.lastUsedAccountId() } returns AccountId("deleted-account")

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("yape", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `OnReset restores last-used account pre-selection`() = runTest(testDispatcher) {
        coEvery { transactionStatsRepository.lastUsedAccountId() } returns AccountId("bcp")

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnAccountSelected(account1))
        advanceUntilIdle()

        vm.onIntent(AddTransactionIntent.OnReset)
        advanceUntilIdle()

        assertEquals("bcp", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `accountSelected is null when account list is empty`() = runTest(testDispatcher) {
        every { accountRepository.all() } returns flowOf(emptyList())

        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.accountSelected)
    }

    @Test
    fun `a category created from a Spend movement joins the Spend list`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val created = SelectableCategory(
            categoryId = CategoryId("subscriptions"),
            name = "Suscripciones",
            iconId = "card",
            categoryType = CategoryType.Spend,
            colorId = "blue",
        )

        vm.onIntent(AddTransactionIntent.OnNewValueFromOthers(created))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(TransactionType.Spend, state.transactionType)
        assertEquals(created, state.categorySelected)
        assertEquals(created, state.categories.first(), "the new one is offered first")
        assertTrue(
            state.categories.all { it.categoryType == CategoryType.Spend },
            "a Spend movement must not be offered Income categories: ${'$'}{state.categories}",
        )
    }

    @Test
    fun `a category of the other type is not attached to the movement`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val before = vm.state.value
        val incomeCategory = SelectableCategory(
            categoryId = CategoryId("salary"),
            name = "Sueldo",
            iconId = "money",
            categoryType = CategoryType.Income,
            colorId = "green",
        )

        vm.onIntent(AddTransactionIntent.OnNewValueFromOthers(incomeCategory))
        advanceUntilIdle()

        assertEquals(before.categorySelected, vm.state.value.categorySelected)
        assertTrue(vm.state.value.categories.all { it.categoryType == CategoryType.Spend })
    }

    @Test
    fun `OnPreselectCombo resolves once data arrives when sent before it loads`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        // Sent before the first advanceUntilIdle(): init's combine has not emitted yet.
        vm.onIntent(
            AddTransactionIntent.OnPreselectCombo(
                accountId = "bcp",
                categoryId = "salary",
                type = TransactionType.Income,
            ),
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(TransactionType.Income, state.transactionType)
        assertEquals("bcp", state.accountSelected?.accountId?.value)
        assertEquals("salary", state.categorySelected?.categoryId?.value)
    }

    @Test
    fun `OnPreselectCombo resolves immediately when data is already loaded`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(
            AddTransactionIntent.OnPreselectCombo(
                accountId = "bcp",
                categoryId = "salary",
                type = TransactionType.Income,
            ),
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(TransactionType.Income, state.transactionType)
        assertEquals("bcp", state.accountSelected?.accountId?.value)
        assertEquals("salary", state.categorySelected?.categoryId?.value)
    }

    @Test
    fun `OnPreselectCombo with a missing id keeps the ordinary defaults`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val defaultAccountId = vm.state.value.accountSelected?.accountId?.value

        vm.onIntent(
            AddTransactionIntent.OnPreselectCombo(
                accountId = "deleted-account",
                categoryId = "deleted-category",
                type = TransactionType.Income,
            ),
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(
            TransactionType.Income,
            state.transactionType,
            "the type still switches even when the ids do not resolve",
        )
        assertEquals(defaultAccountId, state.accountSelected?.accountId?.value)
        // category3, not category2/"salary": proves this is the ordinary type-switch default, not a
        // coincidental match against the requested (nonexistent) id.
        assertEquals(category3.categoryId.value, state.categorySelected?.categoryId?.value)
    }

    @Test
    fun `OnPreselectCombo with every field null touches nothing`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        val before = vm.state.value

        vm.onIntent(AddTransactionIntent.OnPreselectCombo(accountId = null, categoryId = null, type = null))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(before.accountSelected, state.accountSelected)
        assertEquals(before.categorySelected, state.categorySelected)
        assertEquals(before.transactionType, state.transactionType)
        assertEquals(before.hasChanges, state.hasChanges)
    }

    @Test
    fun `a repeat OnPreselectCombo does not re-apply and clobber the user's own edit`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val request = AddTransactionIntent.OnPreselectCombo(
            accountId = "bcp",
            categoryId = "salary",
            type = TransactionType.Income,
        )

        vm.onIntent(request)
        advanceUntilIdle()

        // The user picks a different account, then TransactionEntries' LaunchedEffect(key) restarts
        // (rotation, or popping back from CategoryRoute) and resends the identical route-derived intent.
        vm.onIntent(AddTransactionIntent.OnAccountSelected(account1))
        vm.onIntent(request)
        advanceUntilIdle()

        assertEquals("yape", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `an expired preselect does not resolve once the id later appears`() = runTest(testDispatcher) {
        every { accountRepository.all() } returns flowOf(listOf(account1), listOf(account1, account2))

        val vm = buildViewModel()
        vm.onIntent(AddTransactionIntent.OnPreselectCombo(accountId = "bcp", categoryId = null, type = null))
        advanceUntilIdle()

        assertEquals("yape", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `a cleared screen stops init instead of running on past the cancelled last-used read`() =
        runTest(testDispatcher) {
            var accountsRead = false
            every { accountRepository.all() } answers {
                accountsRead = true
                flowOf(listOf(account1, account2))
            }
            coEvery { transactionStatsRepository.lastUsedAccountId() } coAnswers { awaitCancellation() }
            val store = ViewModelStore()

            val vm = buildViewModel()
            store.put("addTransaction", vm)
            runCurrent()
            store.clear()
            advanceUntilIdle()

            assertFalse(accountsRead)
        }

    @Test
    fun `a type switch that diverges from a pending preselect expires it instead of a cross-type save`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()
            // Sent before data loads: registers, and the switch to Income runs immediately.
            vm.onIntent(
                AddTransactionIntent.OnPreselectCombo(
                    accountId = "bcp",
                    categoryId = "salary",
                    type = TransactionType.Income,
                ),
            )
            // The user taps the type toggle themselves before accounts/categories have loaded.
            vm.onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Spend))
            advanceUntilIdle()

            val state = vm.state.value
            assertEquals(TransactionType.Spend, state.transactionType)
            assertEquals("yape", state.accountSelected?.accountId?.value)
            assertEquals(category1.categoryId.value, state.categorySelected?.categoryId?.value)
        }
}
