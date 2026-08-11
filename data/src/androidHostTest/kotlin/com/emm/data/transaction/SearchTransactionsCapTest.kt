package com.emm.data.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Pins the SQL-side cap on search results against a real in-memory SQLite schema.
 *
 * Search is the deliberate cross-month escape hatch of the transactions tab, so it is the one
 * list left that could stream an unbounded table into memory. The cap keeps the worst case at
 * a fixed size; these tests fail if the LIMIT ever falls off the query or the data source
 * stops passing it.
 */
class SearchTransactionsCapTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var dataSource: TransactionLocalDataSource

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        val db = EmmDatabaseData(driver)
        dataSource = TransactionLocalDataSource(db.transactionsQueries, Clock.System)
        exec("PRAGMA foreign_keys=ON")
        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('acc-1', 'Cuenta', 'Bank', 'PEN', 1, 1)",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `search returns at most 200 rows even when more match`() = runTest {
        repeat(205) { index ->
            insert(id = "t-$index", description = "pollo a la brasa $index", order = index)
        }

        val results = dataSource.searchTransactions(query = "pollo", categoryIds = emptySet()).first()

        assertEquals(200, results.size)
    }

    @Test
    fun `the cap keeps the newest matches`() = runTest {
        // 205 rows, one per minute. The cap of 200 has to cut exactly the five oldest.
        repeat(205) { index ->
            insert(id = "t-$index", description = "pollo a la brasa $index", order = index)
        }

        val results = dataSource.searchTransactions(query = "pollo", categoryIds = emptySet()).first()

        // ORDER BY occurredAt DESC is plain lexicographic order on the stored text, which is why
        // the format is fixed-width — so the LIMIT trims the oldest rows, not the newest.
        assertEquals(200, results.size)
        assertTrue(results.none { it.occurredAt < "2026-08-01T00:05:00" })
        assertTrue(results.any { it.occurredAt == "2026-08-04T00:24:00" }, "the newest row must survive")
    }

    @Test
    fun `fewer matches than the cap come back complete`() = runTest {
        repeat(3) { index ->
            insert(id = "t-$index", description = "pollo a la brasa $index", order = index)
        }

        val results = dataSource.searchTransactions(query = "pollo", categoryIds = emptySet()).first()

        assertEquals(3, results.size)
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    /**
     * [order] doubles as the row's position in time: one row per minute, rolling into the next day
     * every hour. A larger [order] is always a later movement — and, because the column is ISO
     * local text, always a larger string too.
     */
    private fun insert(id: String, description: String, order: Int) {
        val occurredAt = "2026-08-%02dT00:%02d:00".format(1 + order / 60, order % 60)
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt) " +
                "VALUES ('$id', 'Spend', 100, '$description', '$occurredAt', NULL, 'acc-1', 1, 1)",
        )
    }
}
