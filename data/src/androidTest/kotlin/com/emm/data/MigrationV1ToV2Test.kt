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

/**
 * Migration test: schema version 1 → 2 (adds recurring_movements).
 *
 * This is the real safety net guaranteeing that shipping the recurring-movements
 * feature does NOT wipe existing users' data. It builds a database exactly as it
 * exists on production (version 1: accounts, categories, transactions only),
 * inserts representative user data, runs the actual SQLDelight migration
 * (EmmDatabaseData.Schema.migrate, which executes 1.sqm), and asserts that:
 *   1. all pre-existing rows survive, and
 *   2. the new recurring_movements table is created and usable.
 *
 * Requires a device/emulator. Run with: ./gradlew connectedDevDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MigrationV1ToV2Test {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    /**
     * The production schema as it shipped at version 1 — recurring_movements did
     * NOT exist yet. accounts/categories/transactions DDL is copied verbatim from
     * the current .sq files (those three tables are unchanged between v1 and v2).
     */
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
        // Open an in-memory database at version 1 (only the three legacy tables).
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

        // Representative user data on the v1 database.
        database.accountsQueries.insert(
            accountId = "A1",
            name = "BCP",
            type = "Bank",
            currency = "PEN",
            updatedAt = 1_000L,
            createdAt = 1_000L,
        )
        database.categoriesQueries.insert(
            categoryId = "C1",
            name = "Sueldo",
            icon = "salary",
            color = "green",
            categoryType = "Income",
            isDefault = false,
            updatedAt = 1_000L,
            createdAt = 1_000L,
        )
        database.transactionsQueries.insert(
            transactionId = "TX1",
            type = "Income",
            amount = 350_000L,
            description = "Sueldo mayo",
            date = 2_000L,
            categoryId = "C1",
            accountId = "A1",
            createdAt = 2_000L,
            updatedAt = 2_000L,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun migration_v1_to_v2_preserves_user_data_and_adds_recurring_table() {
        // Run the REAL migration (executes 1.sqm).
        EmmDatabaseData.Schema.migrate(driver, oldVersion = 1, newVersion = 2)

        // 1. Pre-existing data survives untouched.
        val account = database.accountsQueries.find("A1").executeAsOneOrNull()
        assertNotNull(account, "account must survive the migration")
        assertEquals("BCP", account.name)

        val category = database.categoriesQueries.find("C1").executeAsOneOrNull()
        assertNotNull(category, "category must survive the migration")
        assertEquals("Sueldo", category.name)

        val tx = database.transactionsQueries.find("TX1").executeAsOneOrNull()
        assertNotNull(tx, "transaction must survive the migration")
        assertEquals(350_000L, tx.amount)

        // 2. The new recurring_movements table exists and is usable (FKs to the
        //    surviving account/category resolve).
        database.recurring_movementsQueries.insert(
            id = "RM1",
            name = "Netflix",
            type = "Spend",
            amount = 4_490L,
            description = "",
            categoryId = "C1",
            accountId = "A1",
            frequency = "Monthly",
            dayOfMonth = 5L,
            isActive = 1L,
            lastConfirmedPeriod = null,
            createdAt = 3_000L,
            updatedAt = 3_000L,
        )
        val recurring = database.recurring_movementsQueries.find("RM1").executeAsOneOrNull()
        assertNotNull(recurring, "recurring_movements row must be insertable after migration")
        assertEquals("Netflix", recurring.name)
    }
}
