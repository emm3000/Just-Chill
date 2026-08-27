package com.emm.justchill.hh.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.TransactionLocalDataSource
import com.emm.domain.account.Account
import com.emm.domain.account.AccountRepository
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.transaction.CreateTransactionUseCase
import com.emm.domain.transaction.DeleteTransactionUseCase
import com.emm.domain.transaction.GetTopUsedCategoryIdsUseCase
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.domain.transaction.UpdateTransactionUseCase
import com.emm.justchill.core.time.TodayFlow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The date, driven the whole way down: ViewModel → use case → data source → SQLite, and back.
 *
 * **This test exists because the previous fix's regression test did not do this.** It called the
 * data source directly, handed it the value it expected back, and asserted that SQL had stored
 * what it was given. It passed. Meanwhile the ViewModel and the use case — the two layers it
 * skipped — were between them rewriting the date into a different day, and two reviewers found it
 * only by reading the code.
 *
 * Nothing here is mocked except the collaborators that have nothing to do with a date. The
 * transaction repository is the real one over a real in-memory database, so what is asserted is
 * the value the column actually holds.
 */
class TransactionDateEndToEndTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var transactionRepository: TransactionRepository

    private val lima = TimeZone.of("America/Lima")

    /** 10 August 2026, 14:30 in Lima. Movable so a test can hold a screen across midnight. */
    private class MovableClock(var instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private fun instantAt(date: LocalDate, hour: Int, minute: Int): Instant =
        LocalDateTime(date, LocalTime(hour, minute)).toInstant(lima)

    private val today = LocalDate(2026, Month.AUGUST, 10)
    private val clock = MovableClock(instantAt(today, hour = 14, minute = 30))

    private val account = Account(AccountId("acc-1"), "BCP")
    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(listOf(account))
        coEvery { find(account.accountId) } returns account
    }
    private val categoryRepository = mockk<CategoryRepository> { every { all() } returns flowOf(emptyList()) }
    private val getTopUsedCategoryIds = mockk<GetTopUsedCategoryIdsUseCase> {
        coEvery { this@mockk.invoke(any(), any(), any()) } returns emptyList()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        driver.execute(
            null,
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('acc-1', 'BCP', 'Bank', 'PEN', 1, 1)",
            0,
        )
        transactionRepository = DefaultTransactionRepository(
            TransactionLocalDataSource(db.transactionsQueries, clock),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        driver.close()
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    fun `the day the user picked is the day the column holds`() = runTest(testDispatcher) {
        val vm = addViewModel().awaitReady()

        vm.onIntent(AddTransactionIntent.OnDateSelected(LocalDate(2026, Month.JUNE, 13)))
        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        vm.onIntent(AddTransactionIntent.OnSave)
        assertEquals(AddTransactionEffect.TransactionSaved, vm.effect.first())

        // The picked day, at the wall-clock time of the save. Read straight out of SQLite.
        assertEquals("2026-06-13T14:30:00", storedOccurredAt("tx-1"))
    }

    @Test
    fun `a transaction saved just after midnight is booked on the new day`() = runTest(testDispatcher) {
        val vm = addViewModel().awaitReady()

        // Screen opened at 23:59 on the 10th, saved at 00:05 on the 11th. This booked the movement
        // on the previous day for as long as the date was resolved when the screen opened.
        clock.instant = instantAt(LocalDate(2026, Month.AUGUST, 11), hour = 0, minute = 5)
        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        vm.onIntent(AddTransactionIntent.OnSave)
        assertEquals(AddTransactionEffect.TransactionSaved, vm.effect.first())

        assertEquals("2026-08-11T00:05:00", storedOccurredAt("tx-1"))
    }

    // ── edit ──────────────────────────────────────────────────────────────────

    @Test
    fun `editing only the amount leaves the stored date byte-for-byte identical`() = runTest(testDispatcher) {
        // The CRITICAL, end to end. A fix that only satisfied the storage layer passed its own
        // test here and still corrupted the date, because the corruption happened above it.
        val stored = seedTransaction("2026-03-04T09:15:33")

        val vm = editViewModel().awaitLoaded()
        vm.onIntent(EditTransactionIntent.OnAmountChange("9999"))
        vm.onIntent(EditTransactionIntent.OnSave)
        assertEquals(EditTransactionEffect.TransactionUpdated, vm.effect.first())

        assertEquals(stored, storedOccurredAt("tx-edit"))
        assertEquals(9_999L, db.transactionsQueries.find("tx-edit").executeAsOne().amount)
    }

    @Test
    fun `an evening transaction survives an amount-only edit without moving a day`() = runTest(testDispatcher) {
        // 23:30 is the hour that used to shift forward by one: read as an instant it fell on
        // the next UTC day, and the round trip through midnight never came back.
        val stored = seedTransaction("2026-03-04T23:30:00")

        val vm = editViewModel().awaitLoaded()
        vm.onIntent(EditTransactionIntent.OnAmountChange("9999"))
        vm.onIntent(EditTransactionIntent.OnSave)
        assertEquals(EditTransactionEffect.TransactionUpdated, vm.effect.first())

        assertEquals(stored, storedOccurredAt("tx-edit"))
    }

    @Test
    fun `moving a transaction to another day keeps the hour it was recorded at`() = runTest(testDispatcher) {
        seedTransaction("2026-03-04T09:15:33")

        val vm = editViewModel().awaitLoaded()
        vm.onIntent(EditTransactionIntent.OnDateSelected(LocalDate(2026, Month.JUNE, 13)))
        vm.onIntent(EditTransactionIntent.OnSave)
        assertEquals(EditTransactionEffect.TransactionUpdated, vm.effect.first())

        assertEquals("2026-06-13T09:15:33", storedOccurredAt("tx-edit"))
    }

    @Test
    fun `re-saving the same transaction never drifts`() = runTest(testDispatcher) {
        // Idempotency, which is what an inverse pair of conversions failed to be. Five round trips
        // through the whole stack have to land on the value the first one did.
        seedTransaction("2026-03-04T23:30:00")

        repeat(5) {
            val vm = editViewModel().awaitLoaded()
            vm.onIntent(EditTransactionIntent.OnAmountChange("9999"))
            vm.onIntent(EditTransactionIntent.OnSave)
            assertEquals(EditTransactionEffect.TransactionUpdated, vm.effect.first())
        }

        assertEquals("2026-03-04T23:30:00", storedOccurredAt("tx-edit"))
    }

    @Test
    fun `the stored date reaches the edit screen unchanged, wherever the device is`() = runTest(testDispatcher) {
        seedTransaction("2026-03-04T23:30:00")

        val karachi = editViewModel(zone = TimeZone.of("Asia/Karachi")).awaitLoaded()

        // Same value, other side of the planet — and the day the screen thinks it is has already
        // rolled over there. The row carries no zone to be re-read in.
        assertEquals(LocalDate(2026, Month.MARCH, 4), karachi.state.value.date)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun addViewModel(): AddTransactionViewModel = AddTransactionViewModel(
        createTransaction = CreateTransactionUseCase(
            transactionRepository = transactionRepository,
            uniqueIdProvider = object : UniqueIdProvider {
                override val id: String get() = "tx-1"
            },
            clock = clock,
            zone = lima,
        ),
        getTopUsedCategoryIds = getTopUsedCategoryIds,
        getFrequentCombos = mockk { coEvery { this@mockk.invoke(any(), any(), any()) } returns emptyList() },
        transactionStatsRepository = mockk<TransactionStatsRepository> {
            coEvery { lastUsedAccountId() } returns null
        },
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        todayFlow = todayFlowIn(lima),
        clock = clock,
        zone = lima,
    )

    private fun editViewModel(zone: TimeZone = lima): EditTransactionViewModel = EditTransactionViewModel(
        transactionId = "tx-edit",
        accountRepository = accountRepository,
        categoryRepository = categoryRepository,
        updateTransaction = UpdateTransactionUseCase(transactionRepository, clock, zone),
        transactionRepository = transactionRepository,
        deleteTransaction = mockk<DeleteTransactionUseCase>(relaxed = true),
        getTopUsedCategoryIds = getTopUsedCategoryIds,
        todayFlow = todayFlowIn(zone),
    )

    /**
     * A fake, as `presentation/CLAUDE.md` requires — but one reading the same movable clock the
     * rest of this test drives, so "today" can never disagree with the instant being stored.
     */
    private fun todayFlowIn(zone: TimeZone): TodayFlow = object : TodayFlow {
        override fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

        override fun invoke(): Flow<LocalDate> = flowOf(today())
    }

    /**
     * The writes run on `Dispatchers.IO`, which the test scheduler does not drive — so these wait
     * on the ViewModel's own signals rather than on `advanceUntilIdle`, which would return while
     * the row was still in flight and make every assertion below it a coin toss.
     */
    private suspend fun AddTransactionViewModel.awaitReady(): AddTransactionViewModel {
        state.first { it.accountSelected != null }
        return this
    }

    private suspend fun EditTransactionViewModel.awaitLoaded(): EditTransactionViewModel {
        state.first { it.accountSelected != null }
        return this
    }

    /** Seeds a row through the real write path and returns the text it stored. */
    private suspend fun seedTransaction(occurredAt: String): String {
        db.transactionsQueries.insert(
            transactionId = "tx-edit",
            type = "Spend",
            amount = 8540L,
            description = "Mercado",
            occurredAt = occurredAt,
            categoryId = null,
            accountId = "acc-1",
            createdAt = 1L,
            updatedAt = 1L,
        )
        assertNotNull(transactionRepository.find(TransactionId("tx-edit")))
        return occurredAt
    }

    private fun storedOccurredAt(id: String): String = db.transactionsQueries.find(id).executeAsOne().occurredAt
}
