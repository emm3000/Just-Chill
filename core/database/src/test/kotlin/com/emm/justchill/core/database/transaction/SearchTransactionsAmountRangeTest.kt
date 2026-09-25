package com.emm.justchill.core.database.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.justchill.core.database.JustChillDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class SearchTransactionsAmountRangeTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var dataSource: TransactionLocalDataSource

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JustChillDatabase.Schema.create(driver)
        val db = JustChillDatabase(driver)
        dataSource = TransactionLocalDataSource(db.transactionsQueries, Clock.System)
        exec("PRAGMA foreign_keys=ON")
        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('acc-1', 'Cuenta', 'Bank', 'PEN', 1, 1)",
        )
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, updatedAt, createdAt) " +
                "VALUES ('cat-1', 'Comida', 'icon', 'green', 'Spend', 1, 1)",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insert(
        id: String,
        amount: Long,
        occurredAt: String = "2026-08-01T00:00:00",
        categoryId: String? = null,
        deletedAt: Long? = null,
    ) {
        val categorySql = categoryId?.let { "'$it'" } ?: "NULL"
        val deletedSql = deletedAt?.toString() ?: "NULL"
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', 'Spend', $amount, 'desc', '$occurredAt', $categorySql, 'acc-1', 1, 1, $deletedSql)",
        )
    }

    private fun search(
        query: String = "",
        categoryIds: Set<String> = emptySet(),
        minAmount: Long? = null,
        maxAmount: Long? = null,
    ): List<String> = runBlocking {
        dataSource.searchTransactions(
            query = query,
            categoryIds = categoryIds,
            minAmountCents = minAmount,
            maxAmountCents = maxAmount,
        ).first().map { it.transactionId }
    }

    @Test
    fun `a row equal to the minimum bound comes back`() {
        insert("t-min", amount = 2_000L)
        insert("t-below", amount = 1_999L)

        val results = search(minAmount = 2_000L)

        assertEquals(listOf("t-min"), results)
    }

    @Test
    fun `a row equal to the maximum bound comes back`() {
        insert("t-max", amount = 5_000L)
        insert("t-above", amount = 5_001L)

        val results = search(maxAmount = 5_000L)

        assertEquals(listOf("t-max"), results)
    }

    @Test
    fun `one cent outside either bound does not come back`() {
        insert("t-in", amount = 3_000L)
        insert("t-below", amount = 1_999L)
        insert("t-above", amount = 5_001L)

        val results = search(minAmount = 2_000L, maxAmount = 5_000L)

        assertEquals(listOf("t-in"), results)
    }

    @Test
    fun `min only excludes rows under it and keeps everything above`() {
        insert("t-below", amount = 1_000L)
        insert("t-at", amount = 2_000L)
        insert("t-above", amount = 9_000L)

        val results = search(minAmount = 2_000L)

        assertEquals(setOf("t-at", "t-above"), results.toSet())
    }

    @Test
    fun `max only excludes rows over it and keeps everything below`() {
        insert("t-below", amount = 1_000L)
        insert("t-at", amount = 5_000L)
        insert("t-above", amount = 9_000L)

        val results = search(maxAmount = 5_000L)

        assertEquals(setOf("t-below", "t-at"), results.toSet())
    }

    @Test
    fun `a range combines with a query and a category`() {
        insert("t-match", amount = 3_000L, categoryId = "cat-1")
        exec(
            "UPDATE transactions SET description = 'pollo a la brasa' WHERE transactionId = 't-match'",
        )
        insert("t-wrong-amount", amount = 9_000L, categoryId = "cat-1")
        exec(
            "UPDATE transactions SET description = 'pollo a la brasa' WHERE transactionId = 't-wrong-amount'",
        )
        insert("t-wrong-category", amount = 3_000L)
        exec(
            "UPDATE transactions SET description = 'pollo a la brasa' WHERE transactionId = 't-wrong-category'",
        )

        val results = search(
            query = "pollo",
            categoryIds = setOf("cat-1"),
            minAmount = 2_000L,
            maxAmount = 5_000L,
        )

        assertEquals(listOf("t-match"), results)
    }

    @Test
    fun `a soft-deleted row in range stays out`() {
        insert("t-live", amount = 3_000L)
        insert("t-deleted", amount = 3_000L, deletedAt = 2L)

        val results = search(minAmount = 2_000L, maxAmount = 5_000L)

        assertEquals(listOf("t-live"), results)
    }
}
