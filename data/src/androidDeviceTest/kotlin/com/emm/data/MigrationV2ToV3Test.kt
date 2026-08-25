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
import kotlin.test.assertNull

private fun SqlDriver.exec(sql: String) = execute(null, sql, 0)

private val accountsTableV2 = """
    CREATE TABLE accounts (
        accountId  TEXT NOT NULL PRIMARY KEY,
        name       TEXT NOT NULL,
        type       TEXT NOT NULL DEFAULT 'Bank',
        currency   TEXT NOT NULL DEFAULT 'PEN',
        updatedAt  INTEGER NOT NULL,
        createdAt  INTEGER NOT NULL
    );
""".trimIndent()

private val categoriesTableV2 = """
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
""".trimIndent()

private val transactionsTableV2 = """
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
""".trimIndent()

private val recurringMovementsTableV2 = """
    CREATE TABLE recurring_movements (
        id                  TEXT NOT NULL PRIMARY KEY,
        name                TEXT NOT NULL,
        type                TEXT NOT NULL,
        amount              INTEGER,
        description         TEXT NOT NULL DEFAULT '',
        categoryId          TEXT REFERENCES categories(categoryId) ON DELETE SET NULL,
        accountId           TEXT NOT NULL REFERENCES accounts(accountId) ON DELETE RESTRICT,
        frequency           TEXT NOT NULL DEFAULT 'Monthly',
        dayOfMonth          INTEGER NOT NULL,
        isActive            INTEGER NOT NULL DEFAULT 1,
        lastConfirmedPeriod TEXT,
        createdAt           INTEGER NOT NULL,
        updatedAt           INTEGER NOT NULL
    );
""".trimIndent()

@RunWith(AndroidJUnit4::class)
class MigrationV2ToV3Test {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    private val schemaV2 = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 2

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.exec(accountsTableV2)
            driver.exec(categoriesTableV2)
            driver.exec(transactionsTableV2)
            driver.exec(recurringMovementsTableV2)
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
            schema = schemaV2,
            context = context,
            name = null,
            callback = object : AndroidSqliteDriver.Callback(schema = schemaV2) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
        database = EmmDatabaseData(driver)

        driver.exec(
            """
            INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt)
            VALUES ('A1', 'BCP', 'Bank', 'PEN', 1000, 1000)
            """.trimIndent(),
        )
        driver.exec(
            """
            INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt)
            VALUES ('C1', 'Sueldo', 'salary', 'green', 'Income', 0, 1000, 1000)
            """.trimIndent(),
        )
        driver.exec(
            """
            INSERT INTO transactions(
                transactionId, type, amount, description, date, categoryId, accountId, createdAt, updatedAt
            )
            VALUES ('TX1', 'Income', 350000, 'Sueldo mayo', 2000, 'C1', 'A1', 2000, 2000)
            """.trimIndent(),
        )
        driver.exec(
            """
            INSERT INTO recurring_movements(
                id, name, type, amount, description, categoryId, accountId,
                frequency, dayOfMonth, isActive, createdAt, updatedAt
            )
            VALUES ('RM1', 'Netflix', 'Spend', 4490, '', 'C1', 'A1', 'Monthly', 5, 1, 3000, 3000)
            """.trimIndent(),
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migration_v2_to_v3_preserves_rows_and_adds_sync_columns() {
        EmmDatabaseData.Schema.migrate(driver, oldVersion = 2, newVersion = EmmDatabaseData.Schema.version)

        val account = database.accountsQueries.find("A1").executeAsOneOrNull()
        assertNotNull(account, "account must survive the migration")
        assertEquals("BCP", account.name)
        assertNull(account.userId, "userId must be null for anonymous-local rows")
        assertNull(account.deletedAt, "deletedAt must be null for existing rows")
        assertEquals("Pending", account.syncState, "syncState must default to 'Pending'")

        val category = database.categoriesQueries.find("C1").executeAsOneOrNull()
        assertNotNull(category, "category must survive the migration")
        assertEquals("Sueldo", category.name)
        assertNull(category.userId)
        assertNull(category.deletedAt)
        assertEquals("Pending", category.syncState)

        val tx = database.transactionsQueries.find("TX1").executeAsOneOrNull()
        assertNotNull(tx, "transaction must survive the migration")
        assertEquals(350_000L, tx.amount)
        assertNull(tx.userId)
        assertNull(tx.deletedAt)
        assertEquals("Pending", tx.syncState)

        val recurring = database.recurring_movementsQueries.find("RM1").executeAsOneOrNull()
        assertNotNull(recurring, "recurring movement must survive the migration")
        assertEquals("Netflix", recurring.name)
        assertNull(recurring.userId)
        assertNull(recurring.deletedAt)
        assertEquals("Pending", recurring.syncState)
    }
}
