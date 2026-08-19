package com.emm.data.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.MonthlyAmountByCategoryAndType
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MonthlyAmountByCategoryAndTypeQueryTest {

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
        insertCategory(id = "cat-live", name = "Comida", categoryType = SPEND, deletedAt = null)
        insertCategory(id = "cat-gone", name = "Antigua", categoryType = SPEND, deletedAt = 500L)
        insertCategory(id = "cat-income", name = "Sueldo", categoryType = INCOME, deletedAt = null)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `income and expense come back as separate rows from one execution`() {
        insertTransaction(id = "t-1", type = SPEND, categoryId = "cat-live", amount = 1_000)
        insertTransaction(id = "t-2", type = INCOME, categoryId = "cat-income", amount = 4_000)

        val rows = monthRows()

        assertEquals(2, rows.size)
        assertEquals(1_000L, rows.single { it.type == SPEND }.totalAmount)
        assertEquals(4_000L, rows.single { it.type == INCOME }.totalAmount)
    }

    @Test
    fun `the two types are never netted against each other`() {
        // The uncategorized pair is what carries the property: the LEFT JOIN collapses every
        // null-category movement into a single group per type, so those two rows share a grouping
        // key in everything except `t.type` — remove it and 900 in is netted against 500 out.
        insertTransaction(id = "t-1", type = SPEND, categoryId = "cat-live", amount = 1_000)
        insertTransaction(id = "t-2", type = SPEND, categoryId = "cat-live", amount = 500)
        insertTransaction(id = "t-3", type = INCOME, categoryId = "cat-income", amount = 700)
        insertTransaction(id = "t-4", type = SPEND, categoryId = null, amount = 500)
        insertTransaction(id = "t-5", type = INCOME, categoryId = null, amount = 900)

        val rows = monthRows()

        assertEquals(1_500L, rows.single { it.type == SPEND && it.categoryId == "cat-live" }.totalAmount)
        assertEquals(700L, rows.single { it.type == INCOME && it.categoryId == "cat-income" }.totalAmount)
        assertEquals(500L, rows.single { it.type == SPEND && it.categoryId == null }.totalAmount)
        assertEquals(900L, rows.single { it.type == INCOME && it.categoryId == null }.totalAmount)
        assertEquals(4, rows.size, "four groups: two categorized, and one uncategorized per type")
    }

    @Test
    fun `each type keeps its own uncategorized bucket`() {
        insertTransaction(id = "t-1", type = SPEND, categoryId = null, amount = 500)
        insertTransaction(id = "t-2", type = SPEND, categoryId = "cat-gone", amount = 300)
        insertTransaction(id = "t-3", type = INCOME, categoryId = null, amount = 900)

        val buckets = monthRows().filter { it.categoryId == null }

        assertEquals(2, buckets.size)
        assertEquals(800L, buckets.single { it.type == SPEND }.totalAmount)
        assertEquals(900L, buckets.single { it.type == INCOME }.totalAmount)
        assertNull(buckets.first().categoryName)
    }

    @Test
    fun `live categories keep their name icon and color`() {
        insertTransaction(id = "t-1", type = SPEND, categoryId = "cat-live", amount = 1_250)

        val row = monthRows().single()

        assertEquals("cat-live", row.categoryId)
        assertEquals("Comida", row.categoryName)
        assertEquals("food", row.categoryIcon)
        assertEquals("green", row.categoryColor)
    }

    @Test
    fun `tombstoned rows and rows outside the window are excluded`() {
        insertTransaction(id = "t-1", type = SPEND, categoryId = "cat-live", amount = 1_000)
        insertTransaction(id = "t-2", type = SPEND, categoryId = "cat-live", amount = 200, deletedAt = 900L)
        insertTransaction(
            id = "t-3",
            type = SPEND,
            categoryId = "cat-live",
            amount = 700,
            occurredAt = JUST_AFTER_MONTH,
        )
        insertTransaction(
            id = "t-4",
            type = SPEND,
            categoryId = "cat-live",
            amount = 400,
            occurredAt = "2026-07-31T23:59:59",
        )

        assertEquals(1_000L, monthRows().single().totalAmount)
    }

    @Test
    fun `per-type rows match what the single-type query returns`() {
        insertTransaction(id = "t-1", type = SPEND, categoryId = "cat-live", amount = 1_000)
        insertTransaction(id = "t-2", type = SPEND, categoryId = null, amount = 500)
        insertTransaction(id = "t-3", type = SPEND, categoryId = "cat-gone", amount = 300)
        insertTransaction(id = "t-4", type = INCOME, categoryId = "cat-income", amount = 4_000)

        val batched = monthRows()
            .filter { it.type == SPEND }
            .associate { it.categoryId to it.totalAmount }
        val singleType = db.transactionsQueries
            .monthlyAmountByCategory(type = SPEND, startInclusive = MONTH_START, endExclusive = MONTH_END)
            .executeAsList()
            .associate { it.categoryId to it.totalAmount }

        assertEquals(singleType, batched)
    }

    private fun monthRows(): List<MonthlyAmountByCategoryAndType> = db.transactionsQueries
        .monthlyAmountByCategoryAndType(startInclusive = MONTH_START, endExclusive = MONTH_END)
        .executeAsList()

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insertCategory(id: String, name: String, categoryType: String, deletedAt: Long?) {
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, " +
                "updatedAt, createdAt, deletedAt) " +
                "VALUES ('$id', '$name', 'food', 'green', '$categoryType', 0, 1, 1, ${deletedAt ?: "NULL"})",
        )
    }

    private fun insertTransaction(
        id: String,
        type: String,
        categoryId: String?,
        amount: Long,
        occurredAt: String = IN_MONTH,
        deletedAt: Long? = null,
    ) {
        val category = categoryId?.let { "'$it'" } ?: "NULL"
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', '$type', $amount, '', '$occurredAt', $category, 'acc-1', 1, 1, " +
                "${deletedAt ?: "NULL"})",
        )
    }

    private companion object {
        const val SPEND = "Spend"
        const val INCOME = "Income"

        const val MONTH_START = "2026-08-01"
        const val MONTH_END = "2026-09-01"
        const val IN_MONTH = "2026-08-10T21:47:33"
        const val JUST_AFTER_MONTH = "2026-09-01T00:00:00"
    }
}
