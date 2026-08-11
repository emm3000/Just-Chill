package com.emm.data.sync

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the sync-related SQLDelight queries against a real in-memory SQLite schema.
 *
 * FK enforcement is enabled at the start of each test via PRAGMA foreign_keys=ON.
 * Setup inserts use raw SQL because the generated `insert` queries force syncState='Pending'
 * and never allow setting userId directly — the same pattern used in
 * [com.emm.data.auth.DefaultClaimLocalDataRepositoryTest].
 *
 * Platform note: FK violation exceptions on JVM/JDBC are thrown as
 * [java.sql.SQLException] (wrapping a SQLite error), NOT [android.database.sqlite.SQLiteException].
 * This difference is noted inline where relevant.
 */
class SyncQueriesTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: EmmDatabaseData

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        exec("PRAGMA foreign_keys=ON")
    }

    @After
    fun tearDown() {
        driver.close()
    }

    // =========================================================================
    // Helper functions
    // =========================================================================

    private fun exec(sql: String) {
        driver.execute(identifier = null, sql = sql, parameters = 0)
    }

    private fun query(sql: String): String? = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
        },
        parameters = 0,
    ).value

    private fun queryLong(sql: String): Long? = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
        },
        parameters = 0,
    ).value

    /** Insert an account row via raw SQL so userId can be set freely. */
    private fun insertAccount(
        id: String,
        userId: String? = "user-1",
        syncState: String = "Synced",
        deletedAt: Long? = null,
        updatedAt: Long = 1000L,
    ) {
        val deletedAtSql = if (deletedAt != null) deletedAt.toString() else "NULL"
        val userIdSql = if (userId != null) "'$userId'" else "NULL"
        exec(
            "INSERT INTO accounts(accountId, name, updatedAt, createdAt, userId, deletedAt, syncState) " +
                "VALUES ('$id', 'Acct $id', $updatedAt, 0, $userIdSql, $deletedAtSql, '$syncState')",
        )
    }

    /** Insert a transaction row via raw SQL so userId/syncState can be set freely. */
    private fun insertTransaction(
        id: String,
        accountId: String = "acc-1",
        userId: String? = "user-1",
        syncState: String = "Synced",
        updatedAt: Long = 1000L,
        deletedAt: Long? = null,
    ) {
        val deletedAtSql = if (deletedAt != null) deletedAt.toString() else "NULL"
        val userIdSql = if (userId != null) "'$userId'" else "NULL"
        exec(
            "INSERT INTO transactions" +
                "(transactionId, type, amount, occurredAt, createdAt, updatedAt, accountId, userId, " +
                "deletedAt, syncState) " +
                "VALUES ('$id', 'Spend', 100, '2026-08-10T12:00:00', 0, $updatedAt, '$accountId', $userIdSql, " +
                "$deletedAtSql, '$syncState')",
        )
    }

    // =========================================================================
    // Two-statement upsert semantics (insertOrIgnoreFromRemote + updateFromRemote)
    // =========================================================================

    @Test
    fun `insertOrIgnoreFromRemote on an existing PK is a no-op`() {
        insertAccount("acc-1", updatedAt = 500L)

        // Attempt to insert the same PK with different data — must be silently ignored.
        db.accountsQueries.insertOrIgnoreFromRemote(
            accountId = "acc-1",
            name = "New Name",
            type = "Bank",
            currency = "PEN",
            updatedAt = 9999L,
            createdAt = 0L,
            userId = "user-1",
            deletedAt = null,
        )

        // Original row must be untouched.
        val name = query("SELECT name FROM accounts WHERE accountId = 'acc-1'")
        assertEquals("Acct acc-1", name)
    }

    @Test
    fun `updateFromRemote updates existing row in place`() {
        insertAccount("acc-1", updatedAt = 500L)

        db.accountsQueries.updateFromRemote(
            name = "Updated Name",
            type = "Cash",
            currency = "USD",
            updatedAt = 9999L,
            createdAt = 0L,
            userId = "user-1",
            deletedAt = null,
            accountId = "acc-1",
        )

        val name = query("SELECT name FROM accounts WHERE accountId = 'acc-1'")
        assertEquals("Updated Name", name)
        val updatedAt = queryLong("SELECT updatedAt FROM accounts WHERE accountId = 'acc-1'")
        assertEquals(9999L, updatedAt)
    }

    @Test
    fun `updateFromRemote on parent account does not delete child transactions`() {
        // This pins the critical FK behaviour: INSERT OR REPLACE would DELETE + INSERT the parent,
        // triggering ON DELETE RESTRICT on transaction rows. updateFromRemote must not do that.
        insertAccount("acc-1")
        insertTransaction("tx-1", accountId = "acc-1")

        db.accountsQueries.updateFromRemote(
            name = "Renamed",
            type = "Bank",
            currency = "PEN",
            updatedAt = 5000L,
            createdAt = 0L,
            userId = "user-1",
            deletedAt = null,
            accountId = "acc-1",
        )

        // Child transaction must still exist.
        val txId = query("SELECT transactionId FROM transactions WHERE transactionId = 'tx-1'")
        assertEquals("tx-1", txId)
    }

    // =========================================================================
    // FK constraint: orphan child row must throw
    // =========================================================================

    @Test
    fun `insertOrIgnoreFromRemote of a transaction whose accountId has no local parent throws FK exception`() {
        // ON CONFLICT clauses (OR IGNORE) do not apply to FOREIGN KEY constraints, so the
        // orphan insert MUST throw — this is what the pull's applyRemote skip path relies on.
        // On JVM/JDBC the error surfaces as a java.sql.SQLException; on Android it is
        // android.database.sqlite.SQLiteConstraintException.
        val thrown = assertFailsWith<Exception> {
            db.transactionsQueries.insertOrIgnoreFromRemote(
                transactionId = "orphan-tx",
                type = "Spend",
                amount = 100L,
                description = "",
                occurredAt = "2026-08-10T21:47:33",
                categoryId = null,
                accountId = "missing-acc",
                createdAt = 0L,
                updatedAt = 1000L,
                userId = "user-1",
                deletedAt = null,
            )
        }
        val message = thrown.message?.lowercase().orEmpty()
        assertTrue(
            message.contains("foreign key"),
            "Expected FK constraint message, got: ${thrown.message}",
        )
        val count = queryLong(
            "SELECT COUNT(*) FROM transactions WHERE transactionId = 'orphan-tx'",
        )
        assertEquals(0L, count, "Orphan row must not be inserted")
    }

    // =========================================================================
    // getAccountBalance: unknown-type rows contribute 0 (Fix 1 pin)
    // =========================================================================

    @Test
    fun `getAccountBalance - unknown-type row contributes 0, known types compute correctly`() {
        insertAccount("acc-balance")
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

    // =========================================================================
    // markSynced guard: only flips when updatedAt matches
    // =========================================================================

    @Test
    fun `markSynced flips syncState to Synced when updatedAt matches`() {
        insertAccount("acc-1")
        insertTransaction("tx-1", syncState = "Pending", updatedAt = 1000L)

        db.transactionsQueries.markSynced("tx-1", 1000L)

        val state = query("SELECT syncState FROM transactions WHERE transactionId = 'tx-1'")
        assertEquals("Synced", state)
    }

    @Test
    fun `markSynced does not flip syncState when updatedAt does not match`() {
        insertAccount("acc-1")
        insertTransaction("tx-1", syncState = "Pending", updatedAt = 1000L)

        // Bind a different updatedAt — the guard must prevent the update.
        db.transactionsQueries.markSynced("tx-1", 9999L)

        val state = query("SELECT syncState FROM transactions WHERE transactionId = 'tx-1'")
        assertEquals("Pending", state)
    }

    @Test
    fun `markSynced accounts version — flips when updatedAt matches`() {
        insertAccount("acc-1", syncState = "Pending", updatedAt = 500L)

        db.accountsQueries.markSynced("acc-1", 500L)

        val state = query("SELECT syncState FROM accounts WHERE accountId = 'acc-1'")
        assertEquals("Synced", state)
    }

    @Test
    fun `markSynced accounts version — no-op when updatedAt does not match`() {
        insertAccount("acc-1", syncState = "Pending", updatedAt = 500L)

        db.accountsQueries.markSynced("acc-1", 1L)

        val state = query("SELECT syncState FROM accounts WHERE accountId = 'acc-1'")
        assertEquals("Pending", state)
    }

    // =========================================================================
    // markPendingForResync: sets syncState='Pending' without touching updatedAt
    // =========================================================================

    @Test
    fun `markPendingForResync sets syncState to Pending`() {
        insertAccount("acc-1")
        insertTransaction("tx-1", syncState = "Synced", updatedAt = 1000L)

        db.transactionsQueries.markPendingForResync("tx-1")

        val state = query("SELECT syncState FROM transactions WHERE transactionId = 'tx-1'")
        assertEquals("Pending", state)
    }

    @Test
    fun `markPendingForResync does not change updatedAt`() {
        insertAccount("acc-1")
        insertTransaction("tx-1", syncState = "Synced", updatedAt = 1000L)

        db.transactionsQueries.markPendingForResync("tx-1")

        val updatedAt = queryLong("SELECT updatedAt FROM transactions WHERE transactionId = 'tx-1'")
        assertEquals(1000L, updatedAt)
    }

    @Test
    fun `markPendingForResync accounts version — sets syncState to Pending without changing updatedAt`() {
        insertAccount("acc-1", syncState = "Synced", updatedAt = 750L)

        db.accountsQueries.markPendingForResync("acc-1")

        val state = query("SELECT syncState FROM accounts WHERE accountId = 'acc-1'")
        assertEquals("Pending", state)
        val updatedAt = queryLong("SELECT updatedAt FROM accounts WHERE accountId = 'acc-1'")
        assertEquals(750L, updatedAt)
    }

    // =========================================================================
    // *SyncRevision: the conflict inputs — updatedAt AND syncState, tombstones included
    // =========================================================================

    @Test
    fun `transactionSyncRevision returns updatedAt and syncState for a live row`() {
        insertAccount("acc-1")
        insertTransaction("tx-1", syncState = "Pending", updatedAt = 2000L)

        val result = db.transactionsQueries.transactionSyncRevision("tx-1").executeAsOneOrNull()

        assertNotNull(result)
        assertEquals(2000L, result.updatedAt)
        assertEquals("Pending", result.syncState)
    }

    @Test
    fun `transactionSyncRevision reports a synced row as Synced`() {
        // The resolver reads this to tell "the user changed this" from "this is just the
        // server's own copy". Getting it wrong here re-arms the clock-skew overwrite loop.
        insertAccount("acc-1")
        insertTransaction("tx-1", syncState = "Pending", updatedAt = 2000L)
        db.transactionsQueries.markSynced("tx-1", 2000L)

        val result = db.transactionsQueries.transactionSyncRevision("tx-1").executeAsOneOrNull()

        assertNotNull(result)
        assertEquals("Synced", result.syncState)
    }

    @Test
    fun `transactionSyncRevision returns a tombstoned row`() {
        // Tombstones must be visible to the LWW sync engine — they carry the deletion
        // timestamp as updatedAt so the conflict resolver can apply them correctly.
        insertAccount("acc-1")
        insertTransaction("tx-1", updatedAt = 3000L, deletedAt = 3000L)

        val result = db.transactionsQueries.transactionSyncRevision("tx-1").executeAsOneOrNull()

        assertNotNull(result, "tombstoned rows are sync-visible")
        assertEquals(3000L, result.updatedAt)
    }

    @Test
    fun `transactionSyncRevision returns null for a non-existent row`() {
        val result = db.transactionsQueries.transactionSyncRevision("nonexistent").executeAsOneOrNull()
        assertNull(result)
    }

    @Test
    fun `accountSyncRevision sees a tombstoned row`() {
        insertAccount("acc-1", updatedAt = 5000L, deletedAt = 5000L)

        val result = db.accountsQueries.accountSyncRevision("acc-1").executeAsOneOrNull()

        assertNotNull(result, "tombstoned account rows are sync-visible")
        assertEquals(5000L, result.updatedAt)
    }

    // =========================================================================
    // selectPending: syncState='Pending' AND userId NOT NULL, including tombstones
    // =========================================================================

    @Test
    fun `selectPending returns only Pending rows with non-null userId`() {
        insertAccount("acc-1")
        // Pending with userId → must appear.
        insertTransaction("tx-pending-owned", syncState = "Pending", userId = "user-1")
        // Synced with userId → must NOT appear.
        insertTransaction("tx-synced-owned", syncState = "Synced", userId = "user-1")
        // Pending but userId IS NULL → must NOT appear.
        insertTransaction("tx-pending-anon", syncState = "Pending", userId = null)

        val rows = db.transactionsQueries.selectPending().executeAsList()
        val ids = rows.map { it.transactionId }

        assertEquals(listOf("tx-pending-owned"), ids)
    }

    @Test
    fun `selectPending includes tombstoned rows when Pending and userId set`() {
        insertAccount("acc-1")
        // Soft-deleted (tombstone) + Pending + userId set → must appear (tombstone needs to sync).
        insertTransaction("tx-tombstone", syncState = "Pending", userId = "user-1", deletedAt = 9000L)

        val rows = db.transactionsQueries.selectPending().executeAsList()

        assertTrue(rows.any { it.transactionId == "tx-tombstone" }, "Tombstoned pending row must be in selectPending")
    }

    @Test
    fun `selectPending accounts version — returns only Pending + owned rows`() {
        // Pending with userId → must appear.
        insertAccount("acc-pending-owned", syncState = "Pending", userId = "user-1")
        // Synced with userId → must NOT appear.
        insertAccount("acc-synced-owned", syncState = "Synced", userId = "user-1")
        // Pending but userId IS NULL → must NOT appear.
        insertAccount("acc-pending-anon", syncState = "Pending", userId = null)

        val rows = db.accountsQueries.selectPending().executeAsList()
        val ids = rows.map { it.accountId }

        assertEquals(listOf("acc-pending-owned"), ids)
    }

    @Test
    fun `selectPending accounts version — includes tombstoned rows with userId set`() {
        insertAccount("acc-tomb", syncState = "Pending", userId = "user-1", deletedAt = 8000L)

        val rows = db.accountsQueries.selectPending().executeAsList()

        assertTrue(rows.any { it.accountId == "acc-tomb" }, "Tombstoned pending account must be in selectPending")
    }
}
