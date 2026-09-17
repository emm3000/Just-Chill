package com.emm.justchill.core.database.backup

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.loan.Loan
import com.emm.justchill.core.domain.loan.LoanPayment
import com.emm.justchill.core.domain.loan.PaymentMethod
import com.emm.justchill.core.domain.recurring.Frequency
import com.emm.justchill.core.domain.recurring.RecurringMovement
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.LoanId
import com.emm.justchill.core.domain.shared.LoanPaymentId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.RecurringMovementId
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.shared.backup.ImportStats
import com.emm.justchill.core.domain.shared.backup.LocalSnapshot
import com.emm.justchill.core.domain.transaction.Transaction
import com.emm.justchill.core.domain.transaction.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class SqlDelightSnapshotStoreTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: JustChillDatabase
    private lateinit var store: SqlDelightSnapshotStore

    private val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-14T00:00:00Z")
    }

    private val restoredAt: Long = Instant.parse("2026-08-14T00:00:00Z").toEpochMilliseconds()

    private val cashAccount = Account(
        accountId = AccountId("acc-1"),
        name = "Yape",
        type = AccountType.Cash,
    )

    private val spendCategory = Category(
        categoryId = CategoryId("cat-1"),
        name = "Comida",
        icon = "work",
        color = "#00FF00",
        categoryType = CategoryType.Spend,
    )

    private val lunch = Transaction(
        transactionId = TransactionId("tx-1"),
        type = TransactionType.Spend,
        amount = Money(100_00L),
        description = "Almuerzo",
        occurredAt = LocalDateTime(2026, 5, 23, 9, 33, 20),
        accountId = AccountId("acc-1"),
        categoryId = CategoryId("cat-1"),
    )

    private val rent = RecurringMovement(
        id = RecurringMovementId("rec-1"),
        name = "Alquiler",
        type = TransactionType.Spend,
        amount = Money(1200_00L),
        description = "Depa",
        categoryId = CategoryId("cat-1"),
        accountId = AccountId("acc-1"),
        frequency = Frequency.Monthly,
        dayOfMonth = 5,
        isActive = true,
        lastConfirmedPeriod = null,
        createdAt = 10L,
    )

    private val loanToRosa = Loan(
        id = LoanId("loan-1"),
        personName = "Rosa",
        personKey = "rosa",
        principal = Money(300_00L),
        interestBps = 0,
        totalDue = Money(300_00L),
        note = "",
        lentAt = LocalDateTime(2026, 5, 23, 9, 33, 20),
    )

    private val rosaPayment = LoanPayment(
        id = LoanPaymentId("pay-1"),
        loanId = LoanId("loan-1"),
        amount = Money(50_00L),
        method = PaymentMethod.Cash,
        paidAt = LocalDateTime(2026, 5, 24, 9, 33, 20),
        note = "",
    )

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JustChillDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
        db = JustChillDatabase(driver)
        store = SqlDelightSnapshotStore(db = db, clock = clock)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `export reads every live table`() = runTest {
        store.restore(fullSnapshot())

        val exported: LocalSnapshot = store.export()

        assertEquals(listOf(cashAccount), exported.accounts)
        assertEquals(listOf(spendCategory), exported.categories)
        assertEquals(listOf(lunch), exported.transactions)
        assertEquals(listOf(rent), exported.recurringMovements)
        assertEquals(listOf(loanToRosa), exported.loans)
        assertEquals(listOf(rosaPayment), exported.loanPayments)
    }

    @Test
    fun `restore counts every row it wrote`() = runTest {
        val stats: ImportStats = store.restore(fullSnapshot())

        assertEquals(
            ImportStats(accounts = 1, categories = 1, transactions = 1, recurring = 1, loans = 1, loanPayments = 1),
            stats,
        )
    }

    @Test
    fun `restore replaces the rows already on the device`() = runTest {
        store.restore(fullSnapshot())

        val otherAccount = cashAccount.copy(accountId = AccountId("acc-2"), name = "BCP")
        store.restore(fullSnapshot().copy(accounts = listOf(otherAccount), transactions = emptyList()))

        val exported: LocalSnapshot = store.export()
        assertEquals(listOf(otherAccount), exported.accounts)
        assertTrue(exported.transactions.isEmpty())
    }

    @Test
    fun `a table the snapshot does not carry keeps its rows`() = runTest {
        store.restore(fullSnapshot())

        store.restore(fullSnapshot().copy(recurringMovements = null, loans = null, loanPayments = null))

        val exported: LocalSnapshot = store.export()
        assertEquals(listOf(rent), exported.recurringMovements)
        assertEquals(listOf(loanToRosa), exported.loans)
        assertEquals(listOf(rosaPayment), exported.loanPayments)
    }

    @Test
    fun `a table the snapshot carries empty is emptied`() = runTest {
        store.restore(fullSnapshot())

        val stats: ImportStats = store.restore(fullSnapshot().copy(loans = emptyList(), loanPayments = emptyList()))

        assertEquals(0, stats.loans)
        assertTrue(store.export().loans.orEmpty().isEmpty())
    }

    @Test
    fun `a transaction whose category changed type restores without a category`() = runTest {
        val incomeCategory = spendCategory.copy(categoryType = CategoryType.Income)

        store.restore(fullSnapshot().copy(categories = listOf(incomeCategory)))

        assertNull(store.export().transactions.single().categoryId)
    }

    @Test
    fun `a payment whose loan the snapshot dropped is neither written nor counted`() = runTest {
        val stats: ImportStats = store.restore(fullSnapshot().copy(loans = emptyList()))

        assertEquals(0, stats.loanPayments)
        assertTrue(store.export().loanPayments.orEmpty().isEmpty())
    }

    @Test
    fun `restore stamps every row with the injected clock`() = runTest {
        store.restore(fullSnapshot())

        assertEquals(restoredAt, db.accountsQueries.all().executeAsOne().updatedAt)
    }

    @Test
    fun `a restored account carries the ledger's only currency, whatever the file held`() = runTest {
        db.accountsQueries.insert(
            accountId = "acc-1",
            name = "Yape",
            type = "Cash",
            currency = "USD",
            updatedAt = 1L,
            createdAt = 1L,
        )

        store.restore(fullSnapshot())

        assertEquals("PEN", db.accountsQueries.all().executeAsOne().currency)
    }

    @Test
    fun `latestLocalChangeAt is null on an empty database`() = runTest {
        assertNull(store.latestLocalChangeAt())
    }

    @Test
    fun `latestLocalChangeAt is the newest write across every table`() = runTest {
        store.restore(fullSnapshot())

        assertEquals(restoredAt, store.latestLocalChangeAt())
    }

    private fun fullSnapshot(): LocalSnapshot = LocalSnapshot(
        accounts = listOf(cashAccount),
        categories = listOf(spendCategory),
        transactions = listOf(lunch),
        recurringMovements = listOf(rent),
        loans = listOf(loanToRosa),
        loanPayments = listOf(rosaPayment),
    )
}
