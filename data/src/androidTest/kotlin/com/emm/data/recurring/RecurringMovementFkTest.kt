package com.emm.data.recurring

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emm.data.EmmDatabaseData
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun soft_delete_recurring_movement_excludes_it_from_live_queries() = runTest {
        val now = 2_000L
        database.recurring_movementsQueries.softDelete(
            deletedAt = now,
            updatedAt = now,
            id = "T1",
        )

        val found = database.recurring_movementsQueries.find("T1").executeAsOneOrNull()
        assertNull(found, "tombstoned recurring movement must not appear in find()")

        val count = database.recurring_movementsQueries.countLiveByAccount("A1").executeAsOne()
        assertEquals(0L, count, "tombstoned row must not be counted as live")
    }

    @Test
    fun tombstoned_category_leaves_the_recurring_movement_linked_but_unnamed() = runTest {
        database.categoriesQueries.softDelete(deletedAt = 2_000L, updatedAt = 2_000L, categoryId = "C1")

        val rawCategoryId = driver.executeQuery(
            null,
            "SELECT categoryId FROM recurring_movements WHERE id = 'T1'",
            { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) },
            0,
        ).value
        assertEquals("C1", rawCategoryId, "the link to the deleted category must survive")

        val row = database.recurring_movementsQueries.selectAllWithDetails().executeAsList().single()
        assertNull(row.categoryName, "a tombstoned category must not lend its name")
    }
}
