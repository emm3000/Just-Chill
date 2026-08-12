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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    /**
     * Movable so a test can hold a ViewModel across midnight — the case that made the add screen
     * write the wrong day. Starts at 2026-08-10 14:30 Lima, so "today" never depends on when the
     * suite runs.
     */
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

    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(listOf(account1, account2))
    }

    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(listOf(category1, category2))
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

    // ── the selected date lives in the state ──────────────────────────────────

    @Test
    fun `date starts unset and reads as Hoy`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        // Unset, not "today resolved at construction": nobody has picked a day yet, and which day
        // "Hoy" is has no answer until the transaction is actually saved.
        assertNull(vm.state.value.date)
        assertEquals(today, vm.state.value.today)
        assertEquals("Hoy", vm.state.value.dateLabel)
    }

    // ── the day is resolved when saving, not when the screen opened ───────────

    @Test
    fun `an untouched date saves as the day it is saved on, not the day the screen opened`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        // The screen was opened just before midnight and sat there. Resolving "Hoy" at
        // construction booked the movement on the previous day, silently.
        fixedClock.instant = instantAt(tomorrow, hour = 0, minute = 5)

        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        advanceUntilIdle()
        vm.onIntent(AddTransactionIntent.OnSave)
        advanceUntilIdle()

        val insert = slot<TransactionInsert>()
        coVerify { createTransaction.invoke(capture(insert)) }
        // The day the save happened on, at the hour it happened at.
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
        // The picked day survives the rollover; only the hour comes from the save.
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

        // Every intent re-reads the clock, so a screen left open overnight stops claiming that
        // yesterday is "Hoy" as soon as the user touches anything.
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
        // The day is the user's, the hour is the clock's — one value, composed once, at the save.
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

        // Unset rather than today: the next transaction is dated when it is saved, and the add
        // screen is reset after every save, so it can easily outlive the day it was opened on.
        assertNull(vm.state.value.date)
        assertEquals("Hoy", vm.state.value.dateLabel)
    }

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
        coEvery { transactionStatsRepository.lastUsedAccountId() } returns AccountId("bcp")

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
        coEvery { transactionStatsRepository.lastUsedAccountId() } returns AccountId("deleted-account")

        val vm = buildViewModel()
        advanceUntilIdle()

        // "deleted-account" is not in the loaded list — falls back to first
        assertEquals("yape", vm.state.value.accountSelected?.accountId?.value)
    }

    @Test
    fun `OnReset restores last-used account pre-selection`() = runTest(testDispatcher) {
        coEvery { transactionStatsRepository.lastUsedAccountId() } returns AccountId("bcp")

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
