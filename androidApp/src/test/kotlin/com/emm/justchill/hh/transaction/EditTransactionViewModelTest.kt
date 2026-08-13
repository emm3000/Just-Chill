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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The date half of this suite is the regression net for the bug where the date picker was handed
 * `DateUtils.currentDateInMillis()` instead of the transaction's own day: editing a March
 * transaction opened the calendar on the current month, and the state had no field that could have
 * told the screen otherwise.
 */
class EditTransactionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val lima = TimeZone.of("America/Lima")
    private val today = LocalDate(2026, Month.AUGUST, 10)

    /**
     * Movable so a test can hold the screen across midnight. Starts at 2026-08-10 14:30 Lima, so
     * "today" never depends on when the suite runs.
     */
    private class MovableClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private fun instantAt(date: LocalDate, hour: Int, minute: Int): Instant = Instant.fromEpochMilliseconds(
        LocalDateTime(date, LocalTime(hour, minute)).toInstant(lima).toEpochMilliseconds(),
    )

    private val fixedClock = MovableClock(instantAt(today, hour = 14, minute = 30))

    private val account = Account(AccountId("bcp"), "BCP")

    private val category = Category(
        categoryId = CategoryId("food"),
        name = "Comida",
        icon = "food",
        color = "blue",
        categoryType = CategoryType.Spend,
    )

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
        every { all() } returns flowOf(listOf(account))
    }
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(listOf(category))
    }
    private val transactionRepository = mockk<TransactionRepository>()
    private val updateTransaction = mockk<UpdateTransactionUseCase>(relaxed = true)
    private val deleteTransaction = mockk<DeleteTransactionUseCase>(relaxed = true)
    private val getTopUsedCategoryIds = mockk<GetTopUsedCategoryIdsUseCase>()

    @Before
    fun setupDefaults() {
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns storedTransaction
        coEvery { accountRepository.find(account.accountId) } returns account
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
        clock = fixedClock,
        zone = lima,
    )

    // ── the loaded date reaches the state ─────────────────────────────────────

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
    fun `today comes from the injected clock`() = runTest(testDispatcher) {
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

        fixedClock.instant = instantAt(LocalDate(2026, Month.AUGUST, 11), hour = 0, minute = 5)
        vm.onIntent(EditTransactionIntent.OnAmountChange("9000"))
        advanceUntilIdle()

        // The transaction did not move; the day under it did. A screen left open overnight must
        // stop calling yesterday "Hoy" as soon as the user touches anything.
        assertEquals(today, vm.state.value.date)
        assertEquals("Ayer", vm.state.value.dateLabel)
    }

    @Test
    fun `an evening transaction keeps its own day, in any zone`() = runTest(testDispatcher) {
        // 23:30 was the hour that used to break this: read in the wrong zone it became the next
        // day, and an edit shifted the date forward by one. The stored value carries no zone now,
        // so there is no zone to read it in wrongly — this holds with the device anywhere.
        val evening = storedTransaction.copy(occurredAt = LocalDateTime(marchDay, LocalTime(23, 30)))
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns evening

        val vm = EditTransactionViewModel(
            transactionId = "tx-1",
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            updateTransaction = updateTransaction,
            transactionRepository = transactionRepository,
            deleteTransaction = deleteTransaction,
            getTopUsedCategoryIds = getTopUsedCategoryIds,
            clock = fixedClock,
            zone = TimeZone.of("Asia/Karachi"),
        )
        advanceUntilIdle()

        assertEquals(marchDay, vm.state.value.date)
    }

    // ── picking a new date ────────────────────────────────────────────────────

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

        assertEquals(true, vm.state.value.hasChanges)
        assertEquals(true, vm.state.value.isEnabled)
    }

    @Test
    fun `re-picking the day already loaded leaves save disabled`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnDateSelected(marchDay))
        advanceUntilIdle()

        assertEquals(false, vm.state.value.hasChanges)
        assertEquals(false, vm.state.value.isEnabled)
    }

    // ── what the save actually sends ──────────────────────────────────────────

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
        // The day the user picked, at the hour the movement was recorded — 09:15:33, down to
        // the second. Moving a movement to another day must not restamp when it happened.
        assertEquals(LocalDateTime(newDay, LocalTime(9, 15, 33)), update.captured.occurredAt)
    }

    @Test
    fun `an edit that does not touch the date sends back the stored value, byte for byte`() = runTest(testDispatcher) {
        // THE regression. Twice now, an amount-only edit has silently moved the date: first by
        // a day, then — when that was patched at one end — by re-deriving the instant at the
        // other. There is one field left and the ViewModel hands it straight back, so equality
        // here is structural, not a conversion that happens to round-trip today.
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

    // ── an uncategorized movement stays uncategorized ─────────────────────────

    /**
     * A movement recorded with no category must not acquire one just by being opened.
     *
     * The screen used to fall back to the first category of the list whenever the selection was
     * empty. `recompute()` then compares that against the snapshot, sees a difference, and enables
     * Save — so editing the amount of an uncategorized movement filed it under whatever category
     * happened to sort first, without the user ever touching the field.
     *
     * `4.sqm` is what turns this from a corner case into a live one: it nulls the category of every
     * movement whose `(categoryId, type)` pair the new key refuses, so the author's device holds a
     * set of movements that now open exactly like this one. They are real financial records.
     */
    @Test
    fun `an uncategorized movement does not acquire a category on load`() = runTest(testDispatcher) {
        coEvery { transactionRepository.find(TransactionId("tx-1")) } returns
            storedTransaction.copy(categoryId = null)

        val vm = buildViewModel()
        advanceUntilIdle()

        assertNull(vm.state.value.categorySelected, "nobody picked this")
        assertFalse(vm.state.value.hasChanges, "opening a screen is not an edit")
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
        // Changing the type IS an edit, so Save is expected to enable — but the category the user
        // never had must not arrive with it. The Income category has to exist for this to be able
        // to fail: with an empty list for the new type, `firstOrNull()` is null for the wrong reason.
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
}
