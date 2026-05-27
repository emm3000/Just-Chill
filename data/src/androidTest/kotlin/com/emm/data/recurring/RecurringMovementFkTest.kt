package com.emm.data.recurring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.shared.safeDbCall
import com.emm.domain.shared.error.DomainException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Instrumented FK ON DELETE tests for recurring_movements table.
 * Spec coverage: 11.1 11.2
 *
 * NOTE: These tests require a real device/emulator and SQLite FK enforcement.
 * They cannot be run in a unit test environment.
 * Run with: ./gradlew connectedDevDebugAndroidTest
 *
 * They do NOT block Slice 1 merge — they are written here but executed as a
 * separate CI step with a device available.
 */
@RunWith(AndroidJUnit4::class)
class RecurringMovementFkTest {

    private lateinit var driver: AndroidSqliteDriver
    private lateinit var database: EmmDatabaseData

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        driver = AndroidSqliteDriver(
            schema = EmmDatabaseData.Schema,
            context = context,
            name = null, // in-memory
            callback = object : AndroidSqliteDriver.Callback(schema = EmmDatabaseData.Schema) {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
        database = EmmDatabaseData(driver)
        // Seed: account A1 and category C1
        database.accountsQueries.insert(
            accountId = "A1",
            name = "Test Account",
            type = "Bank",
            currency = "PEN",
            updatedAt = 1_000L,
            createdAt = 1_000L,
        )
        database.categoriesQueries.insert(
            categoryId = "C1",
            name = "Test Category",
            icon = "wallet",
            color = "blue",
            categoryType = "Spend",
            isDefault = false,
            updatedAt = 1_000L,
            createdAt = 1_000L,
        )
        // Template T1 references A1 and C1
        database.recurring_movementsQueries.insert(
            id = "T1",
            name = "Template 1",
            type = "Spend",
            amount = 100_000L,
            description = "",
            categoryId = "C1",
            accountId = "A1",
            frequency = "Monthly",
            dayOfMonth = 15L,
            isActive = 1L,
            lastConfirmedPeriod = null,
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    /**
     * Scenario 11.1 — Delete account referenced by template → RESTRICT violation.
     * SQLite FK: accountId ON DELETE RESTRICT
     */
    @Test
    fun `deleting account referenced by template throws DatabaseError via safeDbCall`() = runTest {
        assertFailsWith<DomainException.DatabaseError> {
            safeDbCall {
                database.accountsQueries.delete("A1")
            }
        }
        // Both A1 and T1 remain unchanged
        val account = database.accountsQueries.find("A1").executeAsOneOrNull()
        assertEquals("A1", account?.accountId)
        val template = database.recurring_movementsQueries.find("T1").executeAsOneOrNull()
        assertEquals("A1", template?.accountId)
    }

    /**
     * Scenario 11.2 — Delete category referenced by template → SET NULL.
     * SQLite FK: categoryId ON DELETE SET NULL
     */
    @Test
    fun `deleting category referenced by template sets template categoryId to null`() = runTest {
        database.categoriesQueries.delete("C1")

        // Template T1 still exists
        val template = database.recurring_movementsQueries.find("T1").executeAsOneOrNull()
        assertEquals("T1", template?.id)
        // categoryId is now null
        assertNull(template?.categoryId)
    }
}
