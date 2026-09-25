package com.emm.justchill.core.database.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.database.LiveTotals
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LiveTotalsQueryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: JustChillDatabase

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JustChillDatabase.Schema.create(driver)
        db = JustChillDatabase(driver)
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
    fun `balance is income minus spend`() {
        insert(id = "t-1", type = "Income", amount = 10_000)
        insert(id = "t-2", type = "Income", amount = 5_000)
        insert(id = "t-3", type = "Spend", amount = 3_000)

        val totals: LiveTotals = totals()

        assertEquals(12_000L, totals.balance)
        assertEquals(3L, totals.movementCount)
    }

    @Test
    fun `balance goes negative when spending exceeds income`() {
        insert(id = "t-1", type = "Income", amount = 1_000)
        insert(id = "t-2", type = "Spend", amount = 4_000)

        assertEquals(-3_000L, totals().balance)
    }

    @Test
    fun `tombstoned rows count for neither the balance nor the movement count`() {
        insert(id = "t-1", type = "Income", amount = 10_000)
        insert(id = "t-2", type = "Spend", amount = 4_000, deletedAt = 900L)

        val totals: LiveTotals = totals()

        assertEquals(10_000L, totals.balance)
        assertEquals(1L, totals.movementCount)
    }

    @Test
    fun `an unreadable type contributes nothing and is not counted`() {
        insert(id = "t-1", type = "Income", amount = 10_000)
        insert(id = "t-2", type = "Transfer", amount = 7_000)

        val totals: LiveTotals = totals()

        assertEquals(10_000L, totals.balance)
        assertEquals(1L, totals.movementCount)
    }

    @Test
    fun `an empty ledger sums to a balance of 0, never an absent one`() {
        val totals: LiveTotals = totals()
        val balance: Long = totals.balance

        assertEquals(0L, balance)
        assertEquals(0L, totals.movementCount)
    }

    @Test
    fun `a ledger holding only soft-deleted movements sums to 0`() {
        insert(id = "t-1", type = "Income", amount = 10_000, deletedAt = 900L)
        insert(id = "t-2", type = "Spend", amount = 4_000, deletedAt = 900L)

        val totals: LiveTotals = totals()
        val balance: Long = totals.balance

        assertEquals(0L, balance)
        assertEquals(0L, totals.movementCount)
    }

    @Test
    fun `a ledger of only unreadable rows reads as having no movements`() {
        insert(id = "t-1", type = "Transfer", amount = 7_000)

        val totals: LiveTotals = totals()

        assertEquals(0L, totals.balance)
        assertEquals(0L, totals.movementCount)
    }

    private fun totals(): LiveTotals = db.transactionsQueries.liveTotals().executeAsOne()

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insert(id: String, type: String, amount: Long, deletedAt: Long? = null) {
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', '$type', $amount, '', '2026-08-10T12:00:00', NULL, 'acc-1', 1, 1, " +
                "${deletedAt ?: "NULL"})",
        )
    }
}
