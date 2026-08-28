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
import com.emm.justchill.core.time.FakeTodayFlow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    private val todayDates = MutableStateFlow(today)

    /**
     * A real midnight: the clock's hour and TodayFlow's day move together, the way a device does.
     * Moving together is exactly why the tests using this cannot tell the two sources apart — the
     * three below point them at different days for that.
     */
    private fun crossMidnightInto(date: LocalDate, hour: Int, minute: Int) {
        fixedClock.instant = instantAt(date, hour, minute)
        todayDates.value = date
    }

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
        todayFlow = FakeTodayFlow(todayDates),
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

        crossMidnightInto(tomorrow, hour = 0, minute = 5)

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

        crossMidnightInto(tomorrow, hour = 0, minute = 5)

        vm.onIntent(AddTransactionIntent.OnSave)
        advanceUntilIdle()

        val insert = slot<TransactionInsert>()
        coVerify { createTransaction.invoke(capture(insert)) }
        assertEquals(LocalDateTime(picked, LocalTime(0, 5)), insert.captured.occurredAt)
    }

    /**
     * The Clock this ViewModel still holds answers "what hour", never "what day" — not for the
     * initial state, not on an interaction, and not at the save. The three tests below pin one
     * of those each, by pointing TodayFlow at a day the clock does not agree with.
     */
    @Test
    fun `today is TodayFlow's day, not the clock's`() = runTest(testDispatcher) {
        val christmas = LocalDate(2026, Month.DECEMBER, 25)
        todayDates.value = christmas

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(christmas, vm.state.value.today)
    }

    @Test
    fun `an interaction re-reads the day from TodayFlow, not from the clock`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(today, vm.state.value.today)

        // Only TodayFlow moves. The clock stays on the 10th, so re-deriving the day from it here
        // answers with yesterday and this fails.
        todayDates.value = tomorrow
        vm.onIntent(AddTransactionIntent.OnAmountChange("1"))
        advanceUntilIdle()

        assertEquals(tomorrow, vm.state.value.today)
    }

    @Test
    fun `an untouched date is saved as TodayFlow's day, at the clock's hour`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        // The two disagree on purpose: the day must come from TodayFlow and the hour from the
        // clock, which is the only split that tells a second date derivation apart from none.
        todayDates.value = tomorrow
        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        advanceUntilIdle()
        vm.onIntent(AddTransactionIntent.OnSave)
        advanceUntilIdle()

        val insert = slot<TransactionInsert>()
        coVerify { createTransaction.invoke(capture(insert)) }
        assertEquals(LocalDateTime(tomorrow, LocalTime(14, 30)), insert.captured.occurredAt)
    }

    @Test
    fun `today catches up on the next interaction after midnight`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(today, vm.state.value.today)

        crossMidnightInto(tomorrow, hour = 0, minute = 5)
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

        crossMidnightInto(tomorrow, hour = 0, minute = 5)
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
    fun `a second OnSave sent before the write resolves does not call createTransaction again`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { createTransaction.invoke(any()) } coAnswers { gate.await() }

            val vm = buildViewModel()
            advanceUntilIdle()

            vm.onIntent(AddTransactionIntent.OnSave)
            vm.onIntent(AddTransactionIntent.OnSave)
            advanceUntilIdle()

            gate.complete(Unit)
            advanceUntilIdle()

            coVerify(exactly = 1) { createTransaction.invoke(any()) }
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
    fun `a preselected id lands as soon as a later catalog emission carries it`() = runTest(testDispatcher) {
        val accounts = MutableStateFlow(listOf(account1))
        every { accountRepository.all() } returns accounts

        val vm = buildViewModel()
        // "bcp" is not in the catalog yet, so the preselect falls back to the only account there is.
        vm.onIntent(AddTransactionIntent.OnPreselectCombo(accountId = "bcp", categoryId = null, type = null))
        advanceUntilIdle()
        assertEquals("yape", vm.state.value.accountSelected?.accountId?.value)

        accounts.value = listOf(account1, account2)
        advanceUntilIdle()

        // No interaction in between: the id was never consumed, only unresolvable, so the account
        // it names is selected the moment the catalog can name it.
        assertEquals("bcp", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `the empty-account CTA condition stays false until the catalog answers`() = runTest(testDispatcher) {
        every { accountRepository.all() } returns flowOf(emptyList())

        val vm = buildViewModel()
        // Nothing has said there are no accounts yet — only that none have arrived.
        assertFalse(vm.state.value.hasNoAccounts, "the empty-state CTA must not flash before the catalog answers")

        advanceUntilIdle()

        assertTrue(vm.state.value.hasNoAccounts)
    }

    @Test
    fun `a category picked under Spend survives a round trip through Income`() = runTest(testDispatcher) {
        val otherSpendCategory = Category(
            categoryId = CategoryId("transport"),
            name = "Transporte",
            icon = "bus",
            color = "red",
            categoryType = CategoryType.Spend,
        )
        every { categoryRepository.all() } returns flowOf(listOf(category1, otherSpendCategory, category3, category2))

        val vm = buildViewModel()
        advanceUntilIdle()

        val picked = vm.state.value.categories.first { it.categoryId == otherSpendCategory.categoryId }
        vm.onIntent(AddTransactionIntent.OnCategorySelected(picked))
        vm.onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Income))
        advanceUntilIdle()
        assertEquals(category3.categoryId.value, vm.state.value.categorySelected?.categoryId?.value)

        vm.onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Spend))
        advanceUntilIdle()

        // The pick is an id the Spend list still resolves; only the Income view of it was missing.
        assertEquals("transport", vm.state.value.categorySelected?.categoryId?.value)
    }

    @Test
    fun `a preselect whose category id is dangling still lands the account id that resolves`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()

            vm.onIntent(
                AddTransactionIntent.OnPreselectCombo(
                    accountId = "bcp",
                    categoryId = "deleted-category",
                    type = TransactionType.Income,
                ),
            )
            advanceUntilIdle()

            val state = vm.state.value
            assertEquals("bcp", state.accountSelected?.accountId?.value)
            // The dangling id falls back to the Income default on its own; it does not take the
            // account down with it.
            assertEquals(category3.categoryId.value, state.categorySelected?.categoryId?.value)
        }

    @Test
    fun `the chip row never shows the previous type's suggestions after a type switch`() = runTest(testDispatcher) {
        val spendCombo = FrequentCombo(AccountId("yape"), CategoryId("food"), TransactionType.Spend)
        coEvery { getFrequentCombos.invoke(TransactionType.Spend, any<Int>(), any<Int>()) } returns listOf(spendCombo)
        coEvery {
            getTopUsedCategoryIds.invoke(TransactionType.Spend, any<Int>(), any<Int>())
        } returns listOf(CategoryId("food"))
        // The Income reads never land, so nothing but the read-time type filter can empty the row.
        val incomeReads = CompletableDeferred<Unit>()
        coEvery { getTopUsedCategoryIds.invoke(TransactionType.Income, any<Int>(), any<Int>()) } coAnswers {
            incomeReads.await()
            emptyList()
        }

        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(listOf("Yape · Comida"), vm.state.value.frequentCombos.map { it.label })
        assertEquals(listOf("food"), vm.state.value.frequentCategoryIds)

        vm.onIntent(AddTransactionIntent.OnTransactionTypeChange(TransactionType.Income))
        advanceUntilIdle()

        assertTrue(vm.state.value.frequentCombos.isEmpty(), "a Spend combo cannot be offered on an Income movement")
        assertTrue(vm.state.value.frequentCategoryIds.isEmpty())

        incomeReads.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `a cleared screen stops init instead of running on past the cancelled last-used read`() =
        runTest(testDispatcher) {
            var accountsRead = false
            every { accountRepository.all() } answers {
                accountsRead = true
                flowOf(listOf(account1, account2))
            }
            var lastUsedReadReached = false
            coEvery { transactionStatsRepository.lastUsedAccountId() } coAnswers {
                lastUsedReadReached = true
                awaitCancellation()
            }
            val store = ViewModelStore()

            val vm = buildViewModel()
            store.put("addTransaction", vm)
            runCurrent()
            store.clear()
            advanceUntilIdle()

            assertTrue(lastUsedReadReached)
            assertFalse(accountsRead)
        }

    @Test
    fun `a type switch after a preselect keeps the account and never offers the other type's category`() =
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
            // An account carries no type, so the preselected one survives the switch.
            assertEquals("bcp", state.accountSelected?.accountId?.value)
            // "salary" is an Income category and is resolved inside the Spend list, where it does
            // not exist — so the Spend default answers instead. A cross-type pair has no encoding.
            assertEquals(category1.categoryId.value, state.categorySelected?.categoryId?.value)
        }

    @Test
    fun `OnSheetRequested opens the requested sheet and OnSheetDismissed closes it`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertNull(vm.state.value.openSheet)

        vm.onIntent(AddTransactionIntent.OnSheetRequested(TransactionSheet.Account))
        advanceUntilIdle()
        assertEquals(TransactionSheet.Account, vm.state.value.openSheet)

        vm.onIntent(AddTransactionIntent.OnSheetDismissed)
        advanceUntilIdle()
        assertNull(vm.state.value.openSheet)
    }
}
