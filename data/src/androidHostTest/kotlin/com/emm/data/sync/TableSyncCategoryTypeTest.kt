package com.emm.data.sync

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emm.data.EmmDatabaseData
import com.emm.domain.sync.ConflictResolver
import com.emm.domain.sync.SyncLogger
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the pull does with a remote row whose category disagrees with the row's own type, now that
 * the schema refuses the pair.
 *
 * The real table syncs over a real SQLite database with **foreign keys ON**, driven through the
 * real [BaseTableSync] page loop; only the page of remote rows is supplied here. The server holds
 * whatever pairs this device pushed while nothing checked them, so every one of these shapes is a
 * row that already exists up there.
 *
 * The two directions are separate defects and neither implies the other:
 *  - a CHILD row arriving with a mismatched category (transactions, recurring_movements) would be
 *    refused, reported as an FK miss and hold the shared cursor — which is a freeze, not a retry,
 *    because a mismatch does not resolve by waiting;
 *  - a PARENT row arriving with a CHANGED type (categories) strands every local movement filed
 *    under it, so the UPDATE itself is refused. Same freeze, from the other end.
 */
class TableSyncCategoryTypeTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var logger: RecordingLogger

    /** Captures the log lines, which are the only trace a repaired row leaves behind. */
    private class RecordingLogger : SyncLogger {
        val warnings = mutableListOf<String>()

        override fun warn(message: String, throwable: Throwable?) {
            warnings += message
        }
    }

    private class PagedTransactionSync(
        db: EmmDatabaseData,
        logger: SyncLogger,
        private val page: List<TransactionRowDto>,
    ) : TransactionTableSync(db, mockk(relaxed = true), logger) {
        override suspend fun fetchRemotePage(
            userId: String,
            overlapCursor: String?,
            after: PullPageKey?,
            limit: Int,
        ): List<TransactionRowDto> = if (after == null) page else emptyList()
    }

    private class PagedRecurringSync(
        db: EmmDatabaseData,
        logger: SyncLogger,
        private val page: List<RecurringMovementRowDto>,
    ) : RecurringMovementTableSync(db, mockk(relaxed = true), logger) {
        override suspend fun fetchRemotePage(
            userId: String,
            overlapCursor: String?,
            after: PullPageKey?,
            limit: Int,
        ): List<RecurringMovementRowDto> = if (after == null) page else emptyList()
    }

    private class PagedCategorySync(db: EmmDatabaseData, logger: SyncLogger, private val page: List<CategoryRowDto>) :
        CategoryTableSync(db, mockk(relaxed = true), logger) {
        override suspend fun fetchRemotePage(
            userId: String,
            overlapCursor: String?,
            after: PullPageKey?,
            limit: Int,
        ): List<CategoryRowDto> = if (after == null) page else emptyList()
    }

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        logger = RecordingLogger()
        // The whole point of this suite: without enforcement every assertion below passes for the
        // wrong reason, because nothing ever refuses the mismatched pair.
        exec("PRAGMA foreign_keys=ON")
        exec(
            "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('acc-1', 'BCP', 'Bank', 'PEN', 1, 1)",
        )
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) " +
                "VALUES ('cat-spend', 'Café', 'coffee', 'brown', 'Spend', 0, 1, 1)",
        )
        exec(
            "INSERT INTO categories(categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) " +
                "VALUES ('cat-income', 'Sueldo', 'salary', 'green', 'Income', 0, 1, 1)",
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    // ── child rows ────────────────────────────────────────────────────────────

    @Test
    fun `a remote transaction whose category has the other type lands uncategorized`() = runTest {
        val page = listOf(remoteTransaction(id = "tx-mismatch", type = "Income", categoryId = "cat-spend"))

        val result = PagedTransactionSync(db, logger, page).pull(USER, null, ConflictResolver())

        val tx = db.transactionsQueries.find("tx-mismatch").executeAsOneOrNull()
        assertNotNull(tx, "the row must land — deferring it would hold the shared cursor forever")
        assertNull(tx.categoryId)
        assertEquals("Income", tx.type, "the type is the truth and is never rewritten to fit")
        // A mismatch does not resolve by waiting, so holding the cursor would be a freeze, not a
        // retry — and the cursor is shared, so it would freeze all four tables.
        assertFalse(result.skippedRows, "a mismatched category must not hold the pull cursor")
    }

    @Test
    fun `a remote transaction whose category matches keeps it`() = runTest {
        // A repair that stripped every category would also pass the test above.
        val page = listOf(remoteTransaction(id = "tx-ok", type = "Spend", categoryId = "cat-spend"))

        PagedTransactionSync(db, logger, page).pull(USER, null, ConflictResolver())

        assertEquals("cat-spend", db.transactionsQueries.find("tx-ok").executeAsOne().categoryId)
    }

    @Test
    fun `dropping a mismatched category says so, and does not blame a foreign key`() = runTest {
        val page = listOf(remoteTransaction(id = "tx-mismatch", type = "Income", categoryId = "cat-spend"))

        PagedTransactionSync(db, logger, page).pull(USER, null, ConflictResolver())

        val line = logger.warnings.single()
        assertTrue(line.contains("tx-mismatch"), "the line must say which row: $line")
        assertTrue(line.contains("wrong type"), "and why: $line")
        assertFalse(line.contains("fk miss"), "an FK miss is a different, recoverable thing: $line")
    }

    @Test
    fun `a remote recurring movement whose category has the other type lands uncategorized`() = runTest {
        // A template mints a transaction of its own type every month, so a mismatched template
        // would keep re-creating the defect the schema now refuses.
        val page = listOf(remoteRecurring(id = "rm-mismatch", type = "Spend", categoryId = "cat-income"))

        PagedRecurringSync(db, logger, page).pull(USER, null, ConflictResolver())

        val rm = db.recurring_movementsQueries.find("rm-mismatch").executeAsOneOrNull()
        assertNotNull(rm)
        assertNull(rm.categoryId)
        assertEquals("Spend", rm.type)
    }

    @Test
    fun `a remote recurring movement whose category matches keeps it`() = runTest {
        val page = listOf(remoteRecurring(id = "rm-ok", type = "Income", categoryId = "cat-income"))

        PagedRecurringSync(db, logger, page).pull(USER, null, ConflictResolver())

        assertEquals("cat-income", db.recurring_movementsQueries.find("rm-ok").executeAsOne().categoryId)
    }

    // ── the parent row ────────────────────────────────────────────────────────

    @Test
    fun `a remote category that changes its type detaches the movements that no longer fit`() = runTest {
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt) " +
                "VALUES ('tx-1', 'Spend', 500, '', '2026-05-23T09:33:20', 'cat-spend', 'acc-1', 1, 1)",
        )
        exec(
            "INSERT INTO recurring_movements(id, name, type, amount, description, categoryId, accountId, " +
                "dayOfMonth, createdAt, updatedAt) " +
                "VALUES ('rm-1', 'Netflix', 'Spend', 4490, '', 'cat-spend', 'acc-1', 5, 1, 1)",
        )
        val page = listOf(remoteCategory(id = "cat-spend", categoryType = "Income"))

        // Without the detach this throws: the parent-key change orphans both children, SQLite
        // refuses the UPDATE, and the pull reads that refusal as "the parent has not arrived yet".
        PagedCategorySync(db, logger, page).pull(USER, null, ConflictResolver())

        assertEquals("Income", db.categoriesQueries.find("cat-spend").executeAsOne().categoryType)
        assertNull(db.transactionsQueries.find("tx-1").executeAsOne().categoryId)
        assertNull(db.recurring_movementsQueries.find("rm-1").executeAsOne().categoryId)
        // Both movements survive: the label goes, the movement does not.
        assertEquals("Spend", db.transactionsQueries.find("tx-1").executeAsOne().type)
        assertEquals("Spend", db.recurring_movementsQueries.find("rm-1").executeAsOne().type)
    }

    @Test
    fun `a remote category that keeps its type leaves its movements attached`() = runTest {
        exec(
            "INSERT INTO transactions(transactionId, type, amount, description, occurredAt, categoryId, " +
                "accountId, createdAt, updatedAt) " +
                "VALUES ('tx-1', 'Spend', 500, '', '2026-05-23T09:33:20', 'cat-spend', 'acc-1', 1, 1)",
        )
        val page = listOf(remoteCategory(id = "cat-spend", categoryType = "Spend"))

        PagedCategorySync(db, logger, page).pull(USER, null, ConflictResolver())

        assertEquals("cat-spend", db.transactionsQueries.find("tx-1").executeAsOne().categoryId)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun exec(sql: String) = driver.execute(identifier = null, sql = sql, parameters = 0)

    private fun remoteTransaction(id: String, type: String, categoryId: String?) = TransactionRowDto(
        transactionId = id,
        type = type,
        amount = 1_000L,
        description = "",
        date = READABLE_DATE,
        categoryId = categoryId,
        accountId = "acc-1",
        createdAt = 1L,
        updatedAt = 1L,
        userId = USER,
        deletedAt = null,
        serverUpdatedAt = "2026-06-10T01:00:00Z",
    )

    private fun remoteRecurring(id: String, type: String, categoryId: String?) = RecurringMovementRowDto(
        id = id,
        name = "Netflix",
        type = type,
        amount = 4_490L,
        description = "",
        categoryId = categoryId,
        accountId = "acc-1",
        frequency = "Monthly",
        dayOfMonth = 5,
        isActive = true,
        lastConfirmedPeriod = null,
        createdAt = 1L,
        updatedAt = 1L,
        userId = USER,
        deletedAt = null,
        serverUpdatedAt = "2026-06-10T01:00:00Z",
    )

    private fun remoteCategory(id: String, categoryType: String) = CategoryRowDto(
        categoryId = id,
        name = "Café",
        icon = "coffee",
        color = "brown",
        categoryType = categoryType,
        isDefault = false,
        updatedAt = 2L,
        createdAt = 1L,
        userId = USER,
        deletedAt = null,
        serverUpdatedAt = "2026-06-10T01:00:00Z",
    )

    private companion object {
        const val USER = "user-1"

        /** 2025-07-31 17:13:20 in Lima — the same value the migration tests convert. */
        const val READABLE_DATE = 1_754_000_000_000L
    }
}
