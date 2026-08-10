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
        dataSource = TransactionLocalDataSource(db.transactionsQueries)
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
            insert(id = "t-$index", description = "pollo a la brasa $index", date = index.toLong())
        }

        val results = dataSource.searchTransactions(query = "pollo", categoryIds = emptySet()).first()

        assertEquals(200, results.size)
    }

    @Test
    fun `the cap keeps the newest matches`() = runTest {
        repeat(205) { index ->
            insert(id = "t-$index", description = "pollo a la brasa $index", date = index.toLong())
        }

        val results = dataSource.searchTransactions(query = "pollo", categoryIds = emptySet()).first()

        // The query orders by date DESC, so the LIMIT must trim the oldest rows, not the newest.
        assertTrue(results.none { it.date < 5L })
    }

    @Test
    fun `fewer matches than the cap come back complete`() = runTest {
        repeat(3) { index ->
            insert(id = "t-$index", description = "pollo a la brasa $index", date = index.toLong())
        }

        val results = dataSource.searchTransactions(query = "pollo", categoryIds = emptySet()).first()

        assertEquals(3, results.size)
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insert(id: String, description: String, date: Long) {
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, date, categoryId, " +
                "accountId, createdAt, updatedAt) " +
                "VALUES ('$id', 'Spend', 100, '$description', $date, NULL, 'acc-1', 1, 1)",
        )
    }
}
