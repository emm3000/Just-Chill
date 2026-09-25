package com.emm.justchill.core.database.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.database.MonthlyStats
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MonthlyStatsQueryTest {

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
    fun `an empty month sums to 0, never an absent total`() {
        val stats: MonthlyStats = spendStats()
        val totalAmount: Long = stats.totalAmount

        assertEquals(0L, totalAmount)
        assertEquals(0L, stats.movementCount)
    }

    @Test
    fun `a month holding only soft-deleted movements sums to 0`() {
        insert(id = "t-1", occurredAt = "2026-08-10T12:00:00", deletedAt = 900L)

        val totalAmount: Long = spendStats().totalAmount

        assertEquals(0L, totalAmount)
    }

    @Test
    fun `movements outside the month do not count toward its total`() {
        insert(id = "t-1", occurredAt = "2026-08-10T12:00:00", amount = 1_200)
        insert(id = "t-2", occurredAt = "2026-09-01T00:00:00", amount = 9_999)

        val stats: MonthlyStats = spendStats()

        assertEquals(1_200L, stats.totalAmount)
        assertEquals(1L, stats.movementCount)
    }

    private fun spendStats(): MonthlyStats = db.transactionsQueries
        .monthlyStats(type = "Spend", startInclusive = MONTH_START, endExclusive = MONTH_END)
        .executeAsOne()

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insert(id: String, occurredAt: String, amount: Long = 500, deletedAt: Long? = null) {
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', 'Spend', $amount, '', '$occurredAt', NULL, 'acc-1', 1, 1, " +
                "${deletedAt ?: "NULL"})",
        )
    }

    private companion object {
        const val MONTH_START: String = "2026-08-01T00:00:00"
        const val MONTH_END: String = "2026-09-01T00:00:00"
    }
}
