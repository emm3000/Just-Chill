package com.emm.data.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.MonthlyAmountByCategory
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins `monthlyAmountByCategory` against a real in-memory SQLite schema.
 *
 * The Report screen builds its month total by folding these rows; Home folds the raw
 * transactions of the same month. The invariant that keeps both screens telling the same
 * truth is therefore: **the rows returned here must sum to the month total**, with rows
 * that have no live category collapsing into a single uncategorized bucket.
 *
 * Setup inserts use raw SQL because the generated `insert` query forces syncState='Pending'
 * and never allows setting deletedAt directly — the same pattern used in [SyncQueriesTest].
 */
class MonthlyAmountByCategoryQueryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        exec("PRAGMA foreign_keys=ON")

        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('acc-1', 'Cuenta', 'Bank', 'PEN', 1, 1)",
        )
        insertCategory(id = "cat-live", name = "Comida", deletedAt = null)
        insertCategory(id = "cat-gone", name = "Antigua", deletedAt = 500L)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    // =========================================================================
    // Tests
    // =========================================================================

    @Test
    fun `transaction without category lands in the uncategorized bucket`() {
        insertTransaction(id = "t-1", categoryId = "cat-live", amount = 1_000)
        insertTransaction(id = "t-2", categoryId = null, amount = 500)

        val rows = monthRows()

        assertEquals(2, rows.size)
        val uncategorized = rows.single { it.categoryId == null }
        assertEquals(500L, uncategorized.totalAmount)
        assertNull(uncategorized.categoryName)
    }

    @Test
    fun `transaction pointing at a soft-deleted category joins the same uncategorized bucket`() {
        insertTransaction(id = "t-1", categoryId = null, amount = 500)
        insertTransaction(id = "t-2", categoryId = "cat-gone", amount = 300)

        val rows = monthRows()

        assertEquals(1, rows.size)
        assertNull(rows.single().categoryId)
        assertEquals(800L, rows.single().totalAmount)
    }

    @Test
    fun `rows sum to the month total so Report cannot disagree with Home`() {
        insertTransaction(id = "t-1", categoryId = "cat-live", amount = 1_000)
        insertTransaction(id = "t-2", categoryId = null, amount = 500)
        insertTransaction(id = "t-3", categoryId = "cat-gone", amount = 300)
        insertTransaction(id = "t-4", categoryId = "cat-live", amount = 200, deletedAt = 900L)
        insertTransaction(id = "t-5", categoryId = "cat-live", amount = 700, occurredAt = MONTH_END)

        val reportTotal = monthRows().sumOf { it.totalAmount ?: 0L }
        val monthTotal = db.transactionsQueries
            .monthlyStats(type = SPEND, startInclusive = MONTH_START, endExclusive = MONTH_END)
            .executeAsOne()
            .totalAmount

        assertEquals(monthTotal, reportTotal)
        assertEquals(1_800L, reportTotal)
    }

    @Test
    fun `live categories keep their own row with name icon and color`() {
        insertTransaction(id = "t-1", categoryId = "cat-live", amount = 1_000)
        insertTransaction(id = "t-2", categoryId = "cat-live", amount = 250)

        val row = monthRows().single()

        assertEquals("cat-live", row.categoryId)
        assertEquals("Comida", row.categoryName)
        assertEquals("food", row.categoryIcon)
        assertEquals("green", row.categoryColor)
        assertEquals(1_250L, row.totalAmount)
    }

    // =========================================================================
    // Helper functions
    // =========================================================================

    private fun monthRows(): List<MonthlyAmountByCategory> = db.transactionsQueries
        .monthlyAmountByCategory(type = SPEND, startInclusive = MONTH_START, endExclusive = MONTH_END)
        .executeAsList()

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insertCategory(id: String, name: String, deletedAt: Long?) {
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, " +
                "updatedAt, createdAt, deletedAt) " +
                "VALUES ('$id', '$name', 'food', 'green', 'Spend', 0, 1, 1, ${deletedAt ?: "NULL"})",
        )
    }

    private fun insertTransaction(
        id: String,
        categoryId: String?,
        amount: Long,
        occurredAt: String = IN_MONTH,
        deletedAt: Long? = null,
    ) {
        val category = categoryId?.let { "'$it'" } ?: "NULL"
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', 'Spend', $amount, '', '$occurredAt', $category, 'acc-1', 1, 1, " +
                "${deletedAt ?: "NULL"})",
        )
    }

    private companion object {
        const val SPEND = "Spend"

        // Half-open day bounds over August 2026, and a movement inside it. Plain strings: the
        // window is a string comparison in SQL now, with no timezone to make it ambiguous.
        const val MONTH_START = "2026-08-01"
        const val MONTH_END = "2026-09-01"
        const val IN_MONTH = "2026-08-10T21:47:33"
    }
}
