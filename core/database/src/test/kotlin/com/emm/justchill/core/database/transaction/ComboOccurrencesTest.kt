package com.emm.justchill.core.database.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.justchill.core.database.JustChillDatabase
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.transaction.ComboOccurrence
import com.emm.justchill.core.domain.transaction.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComboOccurrencesTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: DefaultTransactionStatsRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        JustChillDatabase.Schema.create(driver)
        val db = JustChillDatabase(driver)
        repository = DefaultTransactionStatsRepository(TransactionStatsLocalDataSource(db.transactionsQueries))
        exec("PRAGMA foreign_keys=ON")
        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('acc-1', 'Cuenta', 'Bank', 'PEN', 1, 1)",
        )
        insertCategory("cat-1")
        insertCategory("cat-2")
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `returns every live occurrence in the window with its amount and hour, oldest first`() = runTest {
        insert(id = "t-1", categoryId = "cat-2", amount = 2000L, occurredAt = "2026-08-11T21:00:00")
        insert(id = "t-2", categoryId = "cat-1", amount = 1000L, occurredAt = "2026-08-10T09:00:00")

        val occurrences = repository.comboOccurrences(TransactionType.Spend, startInclusive = "2026-08-01")

        assertEquals(
            listOf(
                ComboOccurrence(AccountId("acc-1"), CategoryId("cat-1"), TransactionType.Spend, Money(1000L), "2026-08-10T09:00:00"),
                ComboOccurrence(AccountId("acc-1"), CategoryId("cat-2"), TransactionType.Spend, Money(2000L), "2026-08-11T21:00:00"),
            ),
            occurrences,
        )
    }

    @Test
    fun `excludes tombstoned and out-of-window rows`() = runTest {
        insert(id = "t-1", categoryId = "cat-1", amount = 1000L, occurredAt = "2026-08-10T09:00:00", deletedAt = 900L)
        insert(id = "t-2", categoryId = "cat-1", amount = 1000L, occurredAt = "2026-07-01T09:00:00")

        val occurrences = repository.comboOccurrences(TransactionType.Spend, startInclusive = "2026-08-01")

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun `excludes rows without a category`() = runTest {
        insert(id = "t-1", categoryId = null, amount = 1000L, occurredAt = "2026-08-10T09:00:00")

        val occurrences = repository.comboOccurrences(TransactionType.Spend, startInclusive = "2026-08-01")

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun `an empty table yields an empty list`() = runTest {
        val occurrences = repository.comboOccurrences(TransactionType.Spend, startInclusive = "2026-08-01")

        assertTrue(occurrences.isEmpty())
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun insertCategory(id: String) {
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, updatedAt, createdAt) " +
                "VALUES ('$id', 'Categoria $id', 'icon', 'green', 'Spend', 1, 1)",
        )
    }

    private fun insert(id: String, categoryId: String?, amount: Long, occurredAt: String, deletedAt: Long? = null) {
        val categoryValue = if (categoryId == null) "NULL" else "'$categoryId'"
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', 'Spend', $amount, '', '$occurredAt', $categoryValue, 'acc-1', 1, 1, " +
                "${deletedAt ?: "NULL"})",
        )
    }
}
