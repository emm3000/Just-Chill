package com.emm.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MigrationV5ToV6Test {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    private val schemaV5 = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 5

        // Four CREATE TABLEs and fourteen indexes, verbatim. Splitting them up would hide the one
        // thing this block is for: being readable as the schema it claims to reproduce.
        @Suppress("LongMethod")
        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.execute(
                null,
                """
                CREATE TABLE accounts (
                    accountId  TEXT NOT NULL PRIMARY KEY,
                    name       TEXT NOT NULL,
                    type       TEXT NOT NULL DEFAULT 'Bank',
                    currency   TEXT NOT NULL DEFAULT 'PEN',
                    updatedAt  INTEGER NOT NULL,
                    createdAt  INTEGER NOT NULL,
                    userId     TEXT,
                    deletedAt  INTEGER,
                    syncState  TEXT NOT NULL DEFAULT 'Pending'
                );
                """.trimIndent(),
                0,
            )
            driver.execute(
                null,
                """
                CREATE TABLE categories (
                    categoryId    TEXT NOT NULL PRIMARY KEY,
                    name          TEXT NOT NULL,
                    icon          TEXT NOT NULL,
                    color         TEXT NOT NULL,
                    categoryType  TEXT NOT NULL,
                    isDefault     INTEGER NOT NULL DEFAULT 0,
                    updatedAt     INTEGER NOT NULL,
                    createdAt     INTEGER NOT NULL,
                    userId        TEXT,
                    deletedAt     INTEGER,
                    syncState     TEXT NOT NULL DEFAULT 'Pending'
                );
                """.trimIndent(),
                0,
            )
            driver.execute(
                null,
                """
                CREATE TABLE transactions (
                    transactionId  TEXT NOT NULL PRIMARY KEY,
                    type           TEXT NOT NULL,
                    amount         INTEGER NOT NULL,
                    description    TEXT NOT NULL DEFAULT '',
                    occurredAt     TEXT NOT NULL,
                    categoryId     TEXT,
                    accountId      TEXT NOT NULL REFERENCES accounts(accountId) ON DELETE RESTRICT,
                    createdAt      INTEGER NOT NULL,
                    updatedAt      INTEGER NOT NULL,
                    userId         TEXT,
                    deletedAt      INTEGER,
                    syncState      TEXT NOT NULL DEFAULT 'Pending',
                    FOREIGN KEY (categoryId, type) REFERENCES categories(categoryId, categoryType)
                );
                """.trimIndent(),
                0,
            )
            driver.execute(
                null,
                """
                CREATE TABLE recurring_movements (
                    id                  TEXT NOT NULL PRIMARY KEY,
                    name                TEXT NOT NULL,
                    type                TEXT NOT NULL,
                    amount              INTEGER,
                    description         TEXT NOT NULL DEFAULT '',
                    categoryId          TEXT,
                    accountId           TEXT NOT NULL REFERENCES accounts(accountId) ON DELETE RESTRICT,
                    frequency           TEXT NOT NULL DEFAULT 'Monthly',
                    dayOfMonth          INTEGER NOT NULL,
                    isActive            INTEGER NOT NULL DEFAULT 1,
                    lastConfirmedPeriod TEXT,
                    createdAt           INTEGER NOT NULL,
                    updatedAt           INTEGER NOT NULL,
                    userId              TEXT,
                    deletedAt           INTEGER,
                    syncState           TEXT NOT NULL DEFAULT 'Pending',
                    FOREIGN KEY (categoryId, type) REFERENCES categories(categoryId, categoryType)
                );
                """.trimIndent(),
                0,
            )
            listOf(
                "CREATE INDEX IF NOT EXISTS accounts_syncstate_idx ON accounts(syncState)",
                "CREATE INDEX IF NOT EXISTS accounts_user_deleted_idx ON accounts(userId, deletedAt)",
                "CREATE INDEX IF NOT EXISTS categories_type_idx ON categories(categoryType)",
                "CREATE UNIQUE INDEX IF NOT EXISTS categories_id_type_uidx " +
                    "ON categories(categoryId, categoryType)",
                "CREATE INDEX IF NOT EXISTS categories_syncstate_idx ON categories(syncState)",
                "CREATE INDEX IF NOT EXISTS categories_user_deleted_idx ON categories(userId, deletedAt)",
                "CREATE INDEX IF NOT EXISTS transactions_account_idx ON transactions(accountId)",
                "CREATE INDEX IF NOT EXISTS transactions_occurred_idx ON transactions(occurredAt)",
                "CREATE INDEX IF NOT EXISTS transactions_category_idx ON transactions(categoryId)",
                "CREATE INDEX IF NOT EXISTS transactions_syncstate_idx ON transactions(syncState)",
                "CREATE INDEX IF NOT EXISTS transactions_user_deleted_idx ON transactions(userId, deletedAt)",
                "CREATE INDEX IF NOT EXISTS recurring_active_idx ON recurring_movements(isActive, name)",
                "CREATE INDEX IF NOT EXISTS recurring_movements_syncstate_idx ON recurring_movements(syncState)",
                "CREATE INDEX IF NOT EXISTS recurring_movements_user_deleted_idx " +
                    "ON recurring_movements(userId, deletedAt)",
            ).forEach { driver.execute(null, it, 0) }
            return QueryResult.Unit
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: app.cash.sqldelight.db.AfterVersion,
        ): QueryResult.Value<Unit> = QueryResult.Unit
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        driver = AndroidSqliteDriver(
            schema = schemaV5,
            context = context,
            name = null,
            callback = object : AndroidSqliteDriver.Callback(schema = schemaV5) {
                // Foreign keys ON for the whole test: the chain then runs the way iOS runs it, and
                // an orphan payment is rejected instead of stored.
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
        database = EmmDatabaseData(driver)

        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('A1', 'BCP', 'Bank', 'PEN', 1000, 1000)",
        )
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) " +
                "VALUES ('C-INCOME', 'Sueldo', 'salary', 'green', 'Income', 0, 1000, 1000)",
        )
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) " +
                "VALUES ('C-SPEND', 'Café', 'coffee', 'brown', 'Spend', 0, 1000, 1000)",
        )
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt) " +
                "VALUES ('TX1', 'Income', 350000, 'Sueldo mayo', '$SEEDED_OCCURRED_AT', 'C-INCOME', 'A1', 2000, 2000)",
        )
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, accountId, " +
                "frequency, dayOfMonth, isActive, createdAt, updatedAt) " +
                "VALUES ('RM1', 'Netflix', 'Spend', 4490, '', 'C-SPEND', 'A1', 'Monthly', 5, 1, 2000, 2000)",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migration_v5_to_current_preserves_every_row_of_every_existing_table() {
        migrate()

        val account = database.accountsQueries.find("A1").executeAsOneOrNull()
        assertNotNull(account, "account must survive the whole chain")
        assertEquals("BCP", account.name)
        assertEquals("Bank", account.type)
        assertEquals("PEN", account.currency)
        assertEquals(1_000L, account.createdAt)

        val category = database.categoriesQueries.find("C-INCOME").executeAsOneOrNull()
        assertNotNull(category, "category must survive the whole chain")
        assertEquals("Sueldo", category.name)
        assertEquals("salary", category.icon)
        assertEquals("green", category.color)
        assertEquals("Income", category.categoryType)

        val transaction = database.transactionsQueries.find("TX1").executeAsOneOrNull()
        assertNotNull(transaction, "transaction must survive the whole chain")
        assertEquals("Income", transaction.type)
        assertEquals(350_000L, transaction.amount)
        assertEquals("Sueldo mayo", transaction.description)
        assertEquals(SEEDED_OCCURRED_AT, transaction.occurredAt)
        assertEquals("C-INCOME", transaction.categoryId)
        assertEquals("A1", transaction.accountId)

        val recurring = database.recurring_movementsQueries.find("RM1").executeAsOneOrNull()
        assertNotNull(recurring, "recurring movement must survive the whole chain")
        assertEquals("Netflix", recurring.name)
        assertEquals("Spend", recurring.type)
        assertEquals(4_490L, recurring.amount)
        assertEquals("C-SPEND", recurring.categoryId)
        assertEquals("A1", recurring.accountId)
        assertEquals(5L, recurring.dayOfMonth)
    }

    @Test
    fun migration_v5_to_current_leaves_both_loan_tables_writable() {
        migrate()

        insertLoan("L1")
        insertLoanPayment("P1", loanId = "L1")

        assertEquals("Marco", rawText("SELECT personName FROM loans WHERE loanId = 'L1'"))
        assertEquals("marco", rawText("SELECT personKey FROM loans WHERE loanId = 'L1'"))
        assertEquals(50_000L, rawLong("SELECT principal FROM loans WHERE loanId = 'L1'"))
        assertEquals(55_000L, rawLong("SELECT totalDue FROM loans WHERE loanId = 'L1'"))
        assertEquals(LENT_AT, rawText("SELECT lentAt FROM loans WHERE loanId = 'L1'"))
        assertEquals("Pending", rawText("SELECT syncState FROM loans WHERE loanId = 'L1'"))

        assertEquals(20_000L, rawLong("SELECT amount FROM loan_payments WHERE paymentId = 'P1'"))
        assertEquals(PAID_AT, rawText("SELECT paidAt FROM loan_payments WHERE paymentId = 'P1'"))
        assertEquals("Pending", rawText("SELECT syncState FROM loan_payments WHERE paymentId = 'P1'"))
        assertEquals(
            1L,
            rawLong("SELECT COUNT(*) FROM loan_payments p JOIN loans l ON l.loanId = p.loanId"),
            "the payment must resolve to the loan it references",
        )
    }

    @Test
    fun a_payment_whose_loan_does_not_exist_is_rejected_after_the_migration() {
        migrate()

        assertFailsWith<SQLiteConstraintException> {
            insertLoanPayment("P-ORPHAN", loanId = "L-GONE")
        }
        assertEquals(0L, rawLong("SELECT COUNT(*) FROM loan_payments WHERE paymentId = 'P-ORPHAN'"))
    }

    @Test
    fun migration_v5_to_current_creates_every_index_the_new_tables_declare() {
        migrate()

        val loanIndexes = indexNames("loans")
        listOf(
            "loans_person_idx",
            "loans_lentat_idx",
            "loans_user_deleted_idx",
        ).forEach { name ->
            assertTrue(name in loanIndexes, "index $name must exist, found: $loanIndexes")
        }

        val paymentIndexes = indexNames("loan_payments")
        listOf(
            "loan_payments_loan_idx",
            "loan_payments_user_deleted_idx",
        ).forEach { name ->
            assertTrue(name in paymentIndexes, "index $name must exist, found: $paymentIndexes")
        }
    }

    private fun migrate() =
        EmmDatabaseData.Schema.migrate(driver, oldVersion = 5, newVersion = EmmDatabaseData.Schema.version)

    private fun exec(sql: String) = driver.execute(null, sql, 0)

    private fun insertLoan(id: String) = exec(
        "INSERT INTO loans(loanId, personName, personKey, principal, interestBps, totalDue, note, lentAt, " +
            "createdAt, updatedAt) " +
            "VALUES ('$id', 'Marco', 'marco', 50000, 1000, 55000, 'Para el pasaje', '$LENT_AT', 3000, 3000)",
    )

    private fun insertLoanPayment(id: String, loanId: String) = exec(
        "INSERT INTO loan_payments(paymentId, loanId, amount, method, paidAt, note, createdAt, updatedAt) " +
            "VALUES ('$id', '$loanId', 20000, 'Cash', '$PAID_AT', '', 4000, 4000)",
    )

    private fun rawLong(sql: String): Long = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0) ?: 0L)
        },
        parameters = 0,
    ).value

    private fun rawText(sql: String): String? = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getString(0))
        },
        parameters = 0,
    ).value

    private fun indexNames(table: String): List<String> = driver.executeQuery(
        identifier = null,
        sql = "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = '$table'",
        mapper = { cursor ->
            val names = mutableListOf<String>()
            while (cursor.next().value) {
                cursor.getString(0)?.let(names::add)
            }
            QueryResult.Value(names.toList())
        },
        parameters = 0,
    ).value

    private companion object {
        const val SEEDED_OCCURRED_AT = "2026-08-10T21:47:33"
        const val LENT_AT = "2026-08-20T09:30:00"
        const val PAID_AT = "2026-08-21T18:00:00"
    }
}
