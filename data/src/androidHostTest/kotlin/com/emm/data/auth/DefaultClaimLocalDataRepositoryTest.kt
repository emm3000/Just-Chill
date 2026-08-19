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
            "INSERT INTO transactions(transactionId, type, amount, occurredAt, createdAt, updatedAt, " +
                "accountId, userId) " +
                "VALUES ('tx-null', 'Spend', 100, '2026-08-10T12:00:00', 0, 0, 'acc-null', NULL)",
        )
        exec(
            "INSERT INTO transactions(transactionId, type, amount, occurredAt, createdAt, updatedAt, " +
                "accountId, userId) " +
                "VALUES ('tx-other', 'Spend', 100, '2026-08-10T12:00:00', 0, 0, 'acc-null', 'other-user')",
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

        assertEquals("me", userIdOf("accounts", "accountId", "acc-null"))
        assertEquals("other-user", userIdOf("accounts", "accountId", "acc-other"))
    }

    @Test
    fun `observeUnclaimedCount sums NULL-userId rows across all four tables`() = runTest {
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
        val count = repository.observeUnclaimedCount().first()
        assertEquals(4L, count)
    }

    @Test
    fun `unclaimAll reverts owned rows to NULL userId and Pending across all four tables`() = runTest {
        // Without a row that starts out Synced, the Pending assertion below would hold trivially.
        exec("UPDATE accounts SET syncState = 'Synced' WHERE accountId = 'acc-other'")

        repository.unclaimAll("other-user")

        assertEquals(null, userIdOf("accounts", "accountId", "acc-other"))
        assertEquals(null, userIdOf("categories", "categoryId", "cat-other"))
        assertEquals(null, userIdOf("transactions", "transactionId", "tx-other"))
        assertEquals(null, userIdOf("recurring_movements", "id", "rec-other"))
        assertEquals("Pending", syncStateOf("accounts", "accountId", "acc-other"))
    }

    @Test
    fun `unclaimAll never touches rows owned by a different user`() = runTest {
        repository.claimAll("me")

        repository.unclaimAll("other-user")

        assertEquals("me", userIdOf("accounts", "accountId", "acc-null"))
        assertEquals("me", userIdOf("categories", "categoryId", "cat-null"))
        assertEquals("me", userIdOf("transactions", "transactionId", "tx-null"))
        assertEquals("me", userIdOf("recurring_movements", "id", "rec-null"))
    }

    @Test
    fun `unclaimAll also unclaims tombstones (deletedAt NOT NULL)`() = runTest {
        exec(
            "INSERT INTO transactions" +
                "(transactionId, type, amount, occurredAt, createdAt, updatedAt, accountId, userId, deletedAt) " +
                "VALUES ('tx-tombstone', 'Spend', 100, '2026-08-10T12:00:00', 0, 0, 'acc-null', 'other-user', 5)",
        )

        repository.unclaimAll("other-user")

        assertEquals(null, userIdOf("transactions", "transactionId", "tx-tombstone"))
    }

    @Test
    fun `unclaimed rows count as anonymous again and are re-claimable`() = runTest {
        repository.unclaimAll("other-user")

        assertEquals(8L, repository.observeUnclaimedCount().first())

        repository.claimAll("new-user")
        assertEquals("new-user", userIdOf("accounts", "accountId", "acc-other"))
    }

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun syncStateOf(table: String, idColumn: String, id: String): String? = driver.executeQuery(
        identifier = null,
        sql = "SELECT syncState FROM $table WHERE $idColumn = '$id'",
        mapper = { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
        },
        parameters = 0,
    ).value

    private fun userIdOf(table: String, idColumn: String, id: String): String? = driver.executeQuery(
        identifier = null,
        sql = "SELECT userId FROM $table WHERE $idColumn = '$id'",
        mapper = { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
        },
        parameters = 0,
    ).value
}
