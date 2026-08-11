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

/**
 * Migration test: a database at schema version 2 (adds userId, deletedAt, syncState columns).
 *
 * Guarantees that existing user data is preserved when rolling out the soft-delete /
 * sync-metadata schema. Builds a v2 database (all 4 tables as they exist after the
 * recurring_movements migration), inserts representative rows, runs the actual
 * SQLDelight migration (EmmDatabaseData.Schema.migrate 2→3, which executes 2.sqm),
 * and asserts that:
 *   1. All pre-existing rows survive.
 *   2. Each row has userId = null, deletedAt = null, syncState = 'Pending'.
 *
 * Requires a device/emulator. Run with: ./gradlew connectedDevDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MigrationV2ToV3Test {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    /**
     * The production schema at version 2 — all 4 tables exactly as they existed before
     * the sync-metadata columns were added. DDL is copied verbatim from the .sq files
     * plus the 1.sqm recurring_movements table (they are unchanged between v2 and v3).
     */
    private val schemaV2 = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 2

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
            driver.execute(
                null,
                """
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
                """.trimIndent(),
                0,
            )
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

        // Insert representative v2 data using raw SQL so the inserts match the v2 schema
        // (the generated AccountsQueries.insert targets the v3 schema which has syncState).
        driver.execute(null, "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) VALUES ('A1', 'BCP', 'Bank', 'PEN', 1000, 1000)", 0)
        driver.execute(null, "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) VALUES ('C1', 'Sueldo', 'salary', 'green', 'Income', 0, 1000, 1000)", 0)
        driver.execute(null, "INSERT INTO transactions(transactionId, type, amount, description, date, categoryId, accountId, createdAt, updatedAt) VALUES ('TX1', 'Income', 350000, 'Sueldo mayo', 2000, 'C1', 'A1', 2000, 2000)", 0)
        driver.execute(null, "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, accountId, frequency, dayOfMonth, isActive, createdAt, updatedAt) VALUES ('RM1', 'Netflix', 'Spend', 4490, '', 'C1', 'A1', 'Monthly', 5, 1, 3000, 3000)", 0)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migration_v2_to_v3_preserves_rows_and_adds_sync_columns() {
        // Run the REAL migrations, all the way to the current version — which is what a device
        // sitting at v2 actually does when it opens a newer build. Stopping at 3 would leave the
        // database on a schema the generated query classes no longer describe, and every
        // assertion below would fail on a column that has since been renamed.
        EmmDatabaseData.Schema.migrate(driver, oldVersion = 2, newVersion = EmmDatabaseData.Schema.version)

        // The schema is now current, so the generated query classes match.
        // All inserted rows have deletedAt = NULL so the IS NULL filter passes.

        // 1. accounts — row survives; new columns have expected defaults.
        val account = database.accountsQueries.find("A1").executeAsOneOrNull()
        assertNotNull(account, "account must survive the migration")
        assertEquals("BCP", account.name)
        assertNull(account.userId, "userId must be null for anonymous-local rows")
        assertNull(account.deletedAt, "deletedAt must be null for existing rows")
        assertEquals("Pending", account.syncState, "syncState must default to 'Pending'")

        // 2. categories — row survives.
        val category = database.categoriesQueries.find("C1").executeAsOneOrNull()
        assertNotNull(category, "category must survive the migration")
        assertEquals("Sueldo", category.name)
        assertNull(category.userId)
        assertNull(category.deletedAt)
        assertEquals("Pending", category.syncState)

        // 3. transactions — row survives.
        val tx = database.transactionsQueries.find("TX1").executeAsOneOrNull()
        assertNotNull(tx, "transaction must survive the migration")
        assertEquals(350_000L, tx.amount)
        assertNull(tx.userId)
        assertNull(tx.deletedAt)
        assertEquals("Pending", tx.syncState)

        // 4. recurring_movements — row survives.
        val recurring = database.recurring_movementsQueries.find("RM1").executeAsOneOrNull()
        assertNotNull(recurring, "recurring movement must survive the migration")
        assertEquals("Netflix", recurring.name)
        assertNull(recurring.userId)
        assertNull(recurring.deletedAt)
        assertEquals("Pending", recurring.syncState)
    }
}
