package com.emm.data.auth

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Verifies the security-critical claim contract against a real SQLite schema (JVM JDBC driver):
 * claimAll stamps ONLY anonymous rows (userId IS NULL) across all four tables, never touches rows
 * already owned by another user, and is idempotent.
 *
 * Uses raw SQL for setup so each row's userId can be controlled directly (the generated `insert`
 * queries force syncState and never set userId).
 */
class DefaultClaimLocalDataRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var repository: DefaultClaimLocalDataRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        repository = DefaultClaimLocalDataRepository(db)

        // One anonymous row (userId NULL) and one owned by someone else, per table.
        exec(
            "INSERT INTO accounts(accountId, name, updatedAt, createdAt, userId) " +
                "VALUES ('acc-null', 'A', 0, 0, NULL)",
        )
        exec(
            "INSERT INTO accounts(accountId, name, updatedAt, createdAt, userId) " +
                "VALUES ('acc-other', 'B', 0, 0, 'other-user')",
        )

        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, updatedAt, createdAt, userId) " +
                "VALUES ('cat-null', 'C', 'i', '#fff', 'Spend', 0, 0, NULL)",
        )
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, updatedAt, createdAt, userId) " +
                "VALUES ('cat-other', 'D', 'i', '#fff', 'Spend', 0, 0, 'other-user')",
        )

        exec(
            "INSERT INTO transactions(transactionId, type, amount, date, createdAt, updatedAt, accountId, userId) " +
                "VALUES ('tx-null', 'Spend', 100, 0, 0, 0, 'acc-null', NULL)",
        )
        exec(
            "INSERT INTO transactions(transactionId, type, amount, date, createdAt, updatedAt, accountId, userId) " +
                "VALUES ('tx-other', 'Spend', 100, 0, 0, 0, 'acc-null', 'other-user')",
        )

        exec(
            "INSERT INTO recurring_movements(id, name, type, accountId, dayOfMonth, createdAt, updatedAt, userId) " +
                "VALUES ('rec-null', 'R', 'Spend', 'acc-null', 1, 0, 0, NULL)",
        )
        exec(
            "INSERT INTO recurring_movements(id, name, type, accountId, dayOfMonth, createdAt, updatedAt, userId) " +
                "VALUES ('rec-other', 'R', 'Spend', 'acc-null', 1, 0, 0, 'other-user')",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `claimAll stamps every anonymous row across all four tables`() = runTest {
        repository.claimAll("me")

        assertEquals("me", userIdOf("accounts", "accountId", "acc-null"))
        assertEquals("me", userIdOf("categories", "categoryId", "cat-null"))
        assertEquals("me", userIdOf("transactions", "transactionId", "tx-null"))
        assertEquals("me", userIdOf("recurring_movements", "id", "rec-null"))
    }

    @Test
    fun `claimAll never touches rows owned by another user`() = runTest {
        repository.claimAll("me")

        assertEquals("other-user", userIdOf("accounts", "accountId", "acc-other"))
        assertEquals("other-user", userIdOf("categories", "categoryId", "cat-other"))
        assertEquals("other-user", userIdOf("transactions", "transactionId", "tx-other"))
        assertEquals("other-user", userIdOf("recurring_movements", "id", "rec-other"))
    }

    @Test
    fun `claimAll is idempotent — a second call re-claims nothing`() = runTest {
        repository.claimAll("me")
        repository.claimAll("second-user")

        // Already-owned rows are immune to the WHERE userId IS NULL guard.
        assertEquals("me", userIdOf("accounts", "accountId", "acc-null"))
        assertEquals("other-user", userIdOf("accounts", "accountId", "acc-other"))
    }

    @Test
    fun `observeUnclaimedCount sums NULL-userId rows across all four tables`() = runTest {
        // setUp already inserted 1 anonymous row per table = 4 total.
        val count = repository.observeUnclaimedCount().first()
        assertEquals(4L, count)
    }

    @Test
    fun `observeUnclaimedCount decreases after claimAll stamps the anonymous rows`() = runTest {
        assertEquals(4L, repository.observeUnclaimedCount().first())

        repository.claimAll("me")

        assertEquals(0L, repository.observeUnclaimedCount().first())
    }

    @Test
    fun `observeUnclaimedCount ignores rows already owned by another user`() = runTest {
        // Only the 4 anonymous rows count; the 4 'other-user' rows must be excluded.
        val count = repository.observeUnclaimedCount().first()
        assertEquals(4L, count)
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun userIdOf(table: String, idColumn: String, id: String): String? = driver.executeQuery(
        identifier = null,
        sql = "SELECT userId FROM $table WHERE $idColumn = '$id'",
        mapper = { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
        },
        parameters = 0,
    ).value
}
