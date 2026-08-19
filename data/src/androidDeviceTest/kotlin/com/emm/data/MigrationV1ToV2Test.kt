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

        driver.execute(null, "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) VALUES ('A1', 'BCP', 'Bank', 'PEN', 1000, 1000)", 0)
        driver.execute(null, "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) VALUES ('C1', 'Sueldo', 'salary', 'green', 'Income', 0, 1000, 1000)", 0)
        driver.execute(null, "INSERT INTO transactions(transactionId, type, amount, description, date, categoryId, accountId, createdAt, updatedAt) VALUES ('TX1', 'Income', 350000, 'Sueldo mayo', 2000, 'C1', 'A1', 2000, 2000)", 0)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migration_v1_to_v2_preserves_user_data_and_adds_recurring_table() {
        EmmDatabaseData.Schema.migrate(driver, oldVersion = 1, newVersion = 2)

        // The reads below stay raw SQL: this run stops at v2, and the generated query classes
        // only ever describe the current schema.
        val accountName = driver.executeQuery(
            null, "SELECT name FROM accounts WHERE accountId = 'A1'",
            { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) }, 0
        ).value
        assertNotNull(accountName, "account must survive the migration")
        assertEquals("BCP", accountName)

        val categoryName = driver.executeQuery(
            null, "SELECT name FROM categories WHERE categoryId = 'C1'",
            { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) }, 0
        ).value
        assertNotNull(categoryName, "category must survive the migration")
        assertEquals("Sueldo", categoryName)

        val txAmount = driver.executeQuery(
            null, "SELECT amount FROM transactions WHERE transactionId = 'TX1'",
            { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null) }, 0
        ).value
        assertNotNull(txAmount, "transaction must survive the migration")
        assertEquals(350_000L, txAmount)

        driver.execute(
            null,
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, accountId, frequency, dayOfMonth, isActive, createdAt, updatedAt) VALUES ('RM1', 'Netflix', 'Spend', 4490, '', 'C1', 'A1', 'Monthly', 5, 1, 3000, 3000)",
            0,
        )
        val recurringName = driver.executeQuery(
            null, "SELECT name FROM recurring_movements WHERE id = 'RM1'",
            { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) }, 0
        ).value
        assertNotNull(recurringName, "recurring_movements row must be insertable after migration")
        assertEquals("Netflix", recurringName)
    }
}
