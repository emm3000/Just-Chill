package com.emm.justchill.core.database.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.domain.shared.TransactionId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class TransactionsByDateRangeTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: DefaultTransactionRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JustChillDatabase.Schema.create(driver)
        val db = JustChillDatabase(driver)
        repository = DefaultTransactionRepository(TransactionLocalDataSource(db.transactionsQueries, Clock.System))
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
    fun `rows outside the range never arrive, both directions`() = runTest {
        insert(id = "t-jul", day = "2026-07-31")
        insert(id = "t-aug", day = "2026-08-10")
        insert(id = "t-sep", day = "2026-09-01")

        val rows = repository.allInRange(startInclusive = "2026-08-01", endExclusive = "2026-09-01").first()

        assertEquals(listOf(TransactionId("t-aug")), rows.map { it.transactionId })
    }

    @Test
    fun `the last instant of the month is inside, the next day's midnight is not`() = runTest {
        insert(id = "t-last", day = "2026-08-31", time = "23:59:59")
        insert(id = "t-next", day = "2026-09-01", time = "00:00:00")

        val rows = repository.allInRange(startInclusive = "2026-08-01", endExclusive = "2026-09-01").first()

        assertEquals(listOf(TransactionId("t-last")), rows.map { it.transactionId })
    }

    @Test
    fun `a tombstoned row inside the range is still excluded`() = runTest {
        insert(id = "t-1", day = "2026-08-10", deletedAt = 900L)

        val rows = repository.allInRange(startInclusive = "2026-08-01", endExclusive = "2026-09-01").first()

        assertTrue(rows.isEmpty())
    }

    @Test
    fun `an empty range yields an empty list, not every live row`() = runTest {
        insert(id = "t-1", day = "2026-08-10")

        val rows = repository.allInRange(startInclusive = "2026-09-01", endExclusive = "2026-09-01").first()

        assertTrue(rows.isEmpty())
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insert(id: String, day: String, time: String = "12:00:00", deletedAt: Long? = null) {
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', 'Spend', 100, '', '${day}T$time', 'acc-1', 1, 1, ${deletedAt ?: "NULL"})",
        )
    }
}
