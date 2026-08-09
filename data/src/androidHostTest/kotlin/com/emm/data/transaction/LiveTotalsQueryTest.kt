package com.emm.data.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.data.LiveTotals
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Pins `liveTotals` against a real in-memory SQLite schema.
 *
 * Home used to fold every transaction ever recorded in memory to get these two numbers. The
 * replacement is only safe if it answers exactly what the fold answered, so each test here is a
 * case the old fold got right and the aggregate must keep getting right.
 */
class LiveTotalsQueryTest {

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

        val totals = totals()

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

        val totals = totals()

        assertEquals(10_000L, totals.balance)
        assertEquals(1L, totals.movementCount)
    }

    @Test
    fun `an unreadable type contributes nothing and is not counted`() {
        // A newer version on another device can sync down a type this build cannot parse. The
        // mappers drop such a row from every list, so counting it here would light up Home with a
        // balance whose movements the user cannot find anywhere.
        insert(id = "t-1", type = "Income", amount = 10_000)
        insert(id = "t-2", type = "Transfer", amount = 7_000)

        val totals = totals()

        assertEquals(10_000L, totals.balance)
        assertEquals(1L, totals.movementCount)
    }

    @Test
    fun `an empty table reports zero rather than null`() {
        val totals = totals()

        assertEquals(0L, totals.balance)
        assertEquals(0L, totals.movementCount)
    }

    @Test
    fun `a ledger of only unreadable rows reads as having no movements`() {
        insert(id = "t-1", type = "Transfer", amount = 7_000)

        val totals = totals()

        assertEquals(0L, totals.balance)
        assertEquals(0L, totals.movementCount)
    }

    private fun totals(): LiveTotals = db.transactionsQueries.liveTotals().executeAsOne()

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insert(id: String, type: String, amount: Long, deletedAt: Long? = null) {
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, date, categoryId, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', '$type', $amount, '', 1000, NULL, 'acc-1', 1, 1, ${deletedAt ?: "NULL"})",
        )
    }
}
