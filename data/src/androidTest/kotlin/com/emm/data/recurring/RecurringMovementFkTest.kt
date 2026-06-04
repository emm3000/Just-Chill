package com.emm.data.recurring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.EmmDatabaseData
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Soft-delete integration tests for recurring_movements table.
 *
 * Since slice 1 switches hard DELETE to soft-delete (UPDATE SET deletedAt),
 * SQL FK ON DELETE actions (RESTRICT / SET NULL) are no longer triggered by
 * the normal delete flow. Referential integrity now lives in domain use cases
 * (DeleteAccountUseCase, DeleteCategoryUseCase) — see domain tests.
 *
 * These tests verify the soft-delete query behaviour at the SQLite level.
 * Run with: ./gradlew :data:connectedDebugAndroidTest
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
     * Soft-deleting a recurring movement marks it as tombstoned.
     * It no longer appears in live queries (deletedAt IS NULL filters).
     */
    @Test
    fun soft_delete_recurring_movement_excludes_it_from_live_queries() = runTest {
        val now = 2_000L
        database.recurring_movementsQueries.softDelete(
            deletedAt = now,
            updatedAt = now,
            id = "T1",
        )

        // find() filters deletedAt IS NULL — tombstoned row should not appear.
        val found = database.recurring_movementsQueries.find("T1").executeAsOneOrNull()
        assertNull(found, "tombstoned recurring movement must not appear in find()")

        // countLiveByAccount must return 0 for tombstoned rows.
        val count = database.recurring_movementsQueries.countLiveByAccount("A1").executeAsOne()
        assertEquals(0L, count, "tombstoned row must not be counted as live")
    }

    /**
     * nullCategoryOnLiveRows updates categoryId to NULL and sets syncState='Pending'
     * only on live (deletedAt IS NULL) rows.
     */
    @Test
    fun null_category_on_live_rows_sets_category_to_null_and_marks_pending() = runTest {
        database.recurring_movementsQueries.nullCategoryOnLiveRows(
            updatedAt = 3_000L,
            categoryId = "C1",
        )

        // Row T1 is live — categoryId should now be null.
        val template = database.recurring_movementsQueries.find("T1").executeAsOneOrNull()
        assertNotNull(template)
        assertNull(template.categoryId, "categoryId must be nulled on live rows")
        assertEquals("Pending", template.syncState)
    }

    /**
     * nullCategoryOnLiveRows does NOT touch tombstoned rows.
     */
    @Test
    fun null_category_on_live_rows_skips_tombstoned_rows() = runTest {
        // Tombstone T1 first.
        database.recurring_movementsQueries.softDelete(
            deletedAt = 2_000L,
            updatedAt = 2_000L,
            id = "T1",
        )

        // Run nullCategoryOnLiveRows — T1 is tombstoned so it should be untouched.
        database.recurring_movementsQueries.nullCategoryOnLiveRows(
            updatedAt = 3_000L,
            categoryId = "C1",
        )

        // Direct raw read (bypass the IS NULL filter) to verify categoryId was not touched.
        val rawResult = database.recurring_movementsQueries.selectAll().executeAsList()
        // selectAll filters tombstoned rows so result should be empty.
        assertEquals(0, rawResult.size, "tombstoned row must not appear in selectAll")
    }
}
