package com.emm.data.transaction

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.domain.shared.CategoryId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Pins `countPerCategory` and its mapping to the domain usage map against a real in-memory
 * SQLite schema.
 *
 * The transactions tab used to rank its category chips by folding every transaction ever
 * recorded, on every emission. The replacement aggregate is only safe if it counts exactly
 * the rows that fold counted: live rows with a category, nothing else.
 */
class CategoryUsageCountsTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: DefaultTransactionRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        val db = EmmDatabaseData(driver)
        repository = DefaultTransactionRepository(TransactionLocalDataSource(db.transactionsQueries, Clock.System))
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
    fun `counts live transactions per category`() = runTest {
        insert(id = "t-1", categoryId = "cat-1")
        insert(id = "t-2", categoryId = "cat-1")
        insert(id = "t-3", categoryId = "cat-1")
        insert(id = "t-4", categoryId = "cat-2")

        val counts = repository.observeCategoryUsageCounts().first()

        assertEquals(mapOf(CategoryId("cat-1") to 3, CategoryId("cat-2") to 1), counts)
    }

    @Test
    fun `tombstoned transactions do not count`() = runTest {
        insert(id = "t-1", categoryId = "cat-1")
        insert(id = "t-2", categoryId = "cat-1", deletedAt = 900L)
        insert(id = "t-3", categoryId = "cat-2", deletedAt = 900L)

        val counts = repository.observeCategoryUsageCounts().first()

        assertEquals(mapOf(CategoryId("cat-1") to 1), counts)
    }

    @Test
    fun `transactions without a category are excluded`() = runTest {
        insert(id = "t-1", categoryId = null)
        insert(id = "t-2", categoryId = null)
        insert(id = "t-3", categoryId = "cat-1")

        val counts = repository.observeCategoryUsageCounts().first()

        assertEquals(mapOf(CategoryId("cat-1") to 1), counts)
    }

    @Test
    fun `an empty table yields an empty map`() = runTest {
        val counts = repository.observeCategoryUsageCounts().first()

        assertTrue(counts.isEmpty())
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

    private fun insert(id: String, categoryId: String?, deletedAt: Long? = null) {
        val categoryValue = if (categoryId == null) "NULL" else "'$categoryId'"
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt, deletedAt) " +
                "VALUES ('$id', 'Spend', 100, '', '2026-08-10T12:00:00', $categoryValue, 'acc-1', 1, 1, " +
                "${deletedAt ?: "NULL"})",
        )
    }
}
