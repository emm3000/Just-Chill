package com.emm.data.account

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Pins `transactionsQueries.getAccountBalance` against a real in-memory SQLite schema — the query
 * [AccountLocalDataSource] reads the account balance from. Moved out of the deleted sync engine's
 * `SyncQueriesTest` (E01-05): this is production surface, not sync-engine surface.
 */
class GetAccountBalanceQueryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('acc-balance', 'Cuenta', 'Bank', 'PEN', 0, 0)",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    @Test
    fun `getAccountBalance - unknown-type row contributes 0, known types compute correctly`() {
        // Income row: +500
        exec(
            "INSERT INTO transactions(transactionId, type, amount, occurredAt, createdAt, updatedAt, accountId, " +
                "syncState) " +
                "VALUES ('tx-income', 'Income', 500, '2026-08-10T12:00:00', 0, 0, 'acc-balance', 'Synced')",
        )
        // Spend row: -200
        exec(
            "INSERT INTO transactions(transactionId, type, amount, occurredAt, createdAt, updatedAt, accountId, " +
                "syncState) " +
                "VALUES ('tx-spend', 'Spend', 200, '2026-08-10T12:00:00', 0, 0, 'acc-balance', 'Synced')",
        )
        // Unknown-casing row: must contribute 0, not -300 (the old ELSE -amount behaviour)
        exec(
            "INSERT INTO transactions(transactionId, type, amount, occurredAt, createdAt, updatedAt, accountId, " +
                "syncState) " +
                "VALUES ('tx-unknown', 'INCOME', 300, '2026-08-10T12:00:00', 0, 0, 'acc-balance', 'Synced')",
        )

        val balance = db.transactionsQueries.getAccountBalance("acc-balance").executeAsOne()

        // Expected: 500 (Income) - 200 (Spend) + 0 (unknown) = 300
        assertEquals(300L, balance, "Unknown-type row must contribute 0 to account balance")
    }
}
