package com.emm.data

import android.content.Context
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
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
class MigrationV1ToV2Test {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    private val schemaV1 = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 1

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
                    createdAt  INTEGER NOT NULL
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
                    createdAt     INTEGER NOT NULL
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
                    date           INTEGER NOT NULL,
                    categoryId     TEXT REFERENCES categories(categoryId) ON DELETE SET NULL,
                    accountId      TEXT NOT NULL REFERENCES accounts(accountId) ON DELETE RESTRICT,
                    createdAt      INTEGER NOT NULL,
                    updatedAt      INTEGER NOT NULL
                );
                """.trimIndent(),
                0,
            )
            driver.execute(null, "CREATE INDEX IF NOT EXISTS categories_type_idx ON categories(categoryType)", 0)
            driver.execute(null, "CREATE INDEX IF NOT EXISTS transactions_account_idx ON transactions(accountId)", 0)
            driver.execute(null, "CREATE INDEX IF NOT EXISTS transactions_date_idx ON transactions(date)", 0)
            driver.execute(null, "CREATE INDEX IF NOT EXISTS transactions_category_idx ON transactions(categoryId)", 0)
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
            schema = schemaV1,
            context = context,
            name = null,
            callback = object : AndroidSqliteDriver.Callback(schema = schemaV1) {
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
                "VALUES ('C1', 'Sueldo', 'salary', 'green', 'Income', 0, 1000, 1000)",
        )
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, date, categoryId, accountId, " +
                "createdAt, updatedAt) " +
                "VALUES ('TX1', 'Income', 350000, 'Sueldo mayo', $SEEDED_AT_EPOCH_MILLIS, 'C1', 'A1', 2000, 2000)",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migration_v1_to_current_preserves_user_data() {
        migrateToCurrentVersion()

        val account = database.accountsQueries.find("A1").executeAsOneOrNull()
        assertNotNull(account, "account must survive the whole chain")
        assertEquals("BCP", account.name)
        assertEquals("Bank", account.type)
        assertEquals("PEN", account.currency)
        assertEquals(1_000L, account.createdAt)

        val category = database.categoriesQueries.find("C1").executeAsOneOrNull()
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
        assertEquals(SEEDED_AT_LIMA_TEXT, transaction.occurredAt)
        assertEquals("C1", transaction.categoryId)
        assertEquals("A1", transaction.accountId)
    }

    @Test
    fun migration_v1_to_current_leaves_the_recurring_table_writable() {
        migrateToCurrentVersion()

        database.recurring_movementsQueries.insert(
            id = "RM1",
            name = "Netflix",
            type = "Spend",
            amount = 4_490L,
            description = "",
            categoryId = null,
            accountId = "A1",
            frequency = "Monthly",
            dayOfMonth = 5L,
            isActive = 1L,
            lastConfirmedPeriod = null,
            createdAt = 3_000L,
            updatedAt = 3_000L,
        )

        val recurring = database.recurring_movementsQueries.find("RM1").executeAsOneOrNull()
        assertNotNull(recurring, "recurring_movements must be writable after the whole chain")
        assertEquals("Netflix", recurring.name)
        assertEquals(4_490L, recurring.amount)
        assertEquals("A1", recurring.accountId)
    }

    private fun migrateToCurrentVersion() =
        EmmDatabaseData.Schema.migrate(driver, oldVersion = 1, newVersion = EmmDatabaseData.Schema.version)

    private fun exec(sql: String) = driver.execute(null, sql, 0)

    private companion object {
        const val SEEDED_AT_EPOCH_MILLIS = 1_754_000_000_000L
        const val SEEDED_AT_LIMA_TEXT = "2025-07-31T17:13:20"
    }
}
