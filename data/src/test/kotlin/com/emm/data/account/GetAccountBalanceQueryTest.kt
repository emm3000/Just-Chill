package com.emm.data.account

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

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
        exec(
            "INSERT INTO transactions(transactionId, type, amount, occurredAt, createdAt, updatedAt, accountId, " +
                "syncState) " +
                "VALUES ('tx-income', 'Income', 500, '2026-08-10T12:00:00', 0, 0, 'acc-balance', 'Synced')",
        )
        exec(
            "INSERT INTO transactions(transactionId, type, amount, occurredAt, createdAt, updatedAt, accountId, " +
                "syncState) " +
                "VALUES ('tx-spend', 'Spend', 200, '2026-08-10T12:00:00', 0, 0, 'acc-balance', 'Synced')",
        )
        exec(
            "INSERT INTO transactions(transactionId, type, amount, occurredAt, createdAt, updatedAt, accountId, " +
                "syncState) " +
                "VALUES ('tx-unknown', 'INCOME', 300, '2026-08-10T12:00:00', 0, 0, 'acc-balance', 'Synced')",
        )

        val balance = db.transactionsQueries.getAccountBalance("acc-balance").executeAsOne()

        assertEquals(300L, balance, "Unknown-type row must contribute 0 to account balance")
    }
}
