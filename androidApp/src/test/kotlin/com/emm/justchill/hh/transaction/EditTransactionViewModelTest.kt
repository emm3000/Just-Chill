package com.emm.justchill.hh.transaction

import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.account.FindAccountUseCase
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.FindTransactionUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.Transaction
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
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
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

    /** Fixed at 2026-08-10 14:30 Lima, so "today" never depends on when the suite runs. */
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(
            LocalDateTime(today, LocalTime(14, 30)).toInstant(lima).toEpochMilliseconds(),
        )
    }

    private val account = Account(AccountId("bcp"), "BCP")

    private val category = Category(
        categoryId = CategoryId("food"),
        name = "Comida",
        icon = "food",
        color = "blue",
        categoryType = CategoryType.Spend,
    )

    /** Recorded on 4 March 2026 at 09:15 Lima — a day that is neither today nor yesterday. */
    private val marchDay = LocalDate(2026, Month.MARCH, 4)
    private val marchMillis = LocalDateTime(marchDay, LocalTime(9, 15)).toInstant(lima).toEpochMilliseconds()

    private val storedTransaction = Transaction(
        transactionId = TransactionId("tx-1"),
        type = TransactionType.Spend,
        amount = Money(8540),
        description = "Mercado",
        date = marchMillis,
        accountId = account.accountId,
        categoryId = category.categoryId,
    )

    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(listOf(account))
    }
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(listOf(category))
    }
    private val updateTransaction = mockk<UpdateTransactionUseCase>(relaxed = true)
    private val findTransaction = mockk<FindTransactionUseCase>()
    private val deleteTransaction = mockk<DeleteTransactionUseCase>(relaxed = true)
    private val findAccount = mockk<FindAccountUseCase>()
    private val getTopUsedCategoryIds = mockk<GetTopUsedCategoryIdsUseCase>()

    @Before
    fun setupDefaults() {
        coEvery { findTransaction.invoke(TransactionId("tx-1")) } returns storedTransaction
        coEvery { findAccount.invoke(account.accountId) } returns account
        coEvery { getTopUsedCategoryIds.invoke(any(), any(), any()) } returns emptyList()
    }

    private fun buildViewModel(): EditTransactionViewModel = EditTransactionViewModel(
        transactionId = "tx-1",
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        updateTransaction = updateTransaction,
        findTransaction = findTransaction,
        deleteTransaction = deleteTransaction,
        findAccount = findAccount,
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
    fun `an evening transaction keeps its own day rather than the UTC one`() = runTest(testDispatcher) {
        // 23:30 in Lima is already the next day in UTC. Resolving the day in the wrong zone is how
        // an edit used to shift the date forward by one.
        val evening = LocalDateTime(marchDay, LocalTime(23, 30)).toInstant(lima).toEpochMilliseconds()
        coEvery { findTransaction.invoke(TransactionId("tx-1")) } returns storedTransaction.copy(date = evening)

        val vm = buildViewModel()
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

    // ── the day reaches the domain as local midnight ──────────────────────────

    @Test
    fun `save sends the picked day as local midnight`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        val newDay = LocalDate(2026, Month.JUNE, 13)
        vm.onIntent(EditTransactionIntent.OnDateSelected(newDay))
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnSave)
        advanceUntilIdle()

        val update = slot<TransactionUpdate>()
        coVerify { updateTransaction.invoke(storedTransaction, capture(update)) }
        assertEquals(newDay.atStartOfDayIn(lima).toEpochMilliseconds(), update.captured.date)
    }

    @Test
    fun `save sends the untouched day back unchanged as local midnight`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnAmountChange("9000"))
        advanceUntilIdle()

        vm.onIntent(EditTransactionIntent.OnSave)
        advanceUntilIdle()

        val update = slot<TransactionUpdate>()
        coVerify { updateTransaction.invoke(storedTransaction, capture(update)) }
        // UpdateTransactionUseCase carries the original time of day back over this midnight —
        // that contract is DateAndTimeCombinerTest's, not this suite's.
        assertEquals(marchDay.atStartOfDayIn(lima).toEpochMilliseconds(), update.captured.date)
    }
}
