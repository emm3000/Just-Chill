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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What a remote transaction the client cannot read actually does, end to end.
 *
 * The real [TransactionTableSync] over a real SQLite database, driven through the real
 * [BaseTableSync] pull loop; only the page of remote rows is supplied by the test. That matters
 * here more than usual: the failure this pins is not in any one of those pieces, it is in what one
 * of them told another. `applyRemoteRow` reported an unreadable `date` the same way it reports an
 * FK miss, the page loop read that as "retry me", and `DefaultSyncRepository` then refused to
 * persist the advanced cursor — for all four tables, forever, because an unreadable value never
 * becomes readable. A test that only exercised the mapper would have seen nothing wrong.
 */
class TransactionTableSyncPullTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: EmmDatabaseData
    private lateinit var logger: RecordingLogger

    /** Captures the log lines, which are the only trace a skipped row leaves behind. */
    private class RecordingLogger : SyncLogger {
        val warnings = mutableListOf<String>()

        override fun warn(message: String, throwable: Throwable?) {
            warnings += message
        }
    }

    /** The production class, with the network call replaced by a fixed page. Nothing else. */
    private class PagedTransactionTableSync(
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

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        EmmDatabaseData.Schema.create(driver)
        db = EmmDatabaseData(driver)
        logger = RecordingLogger()
        driver.execute(
            identifier = null,
            sql = "INSERT INTO accounts(accountId, name, type, currency, updatedAt, createdAt) " +
                "VALUES ('acc-1', 'BCP', 'Bank', 'PEN', 1, 1)",
            parameters = 0,
        )
    }

    @After
    fun tearDown() {
        driver.close()
    }

    @Test
    fun `an unreadable remote date drops its row and lets the cursor move past it`() = runTest {
        val page = listOf(
            remoteRow(id = "tx-good", date = READABLE_DATE, serverUpdatedAt = "2026-06-10T01:00:00Z"),
            remoteRow(id = "tx-bad", date = Long.MAX_VALUE, serverUpdatedAt = "2026-06-10T02:00:00Z"),
            remoteRow(id = "tx-later", date = READABLE_DATE, serverUpdatedAt = "2026-06-10T03:00:00Z"),
        )

        val result = pull(page)

        // The whole finding in one assertion: skippedRows is what holds the cursor, and a row that
        // can never be read must not hold it. `true` here is the permanent freeze.
        assertFalse(result.skippedRows, "an unreadable row must not hold the pull cursor")
        assertEquals(
            java.time.Instant.parse("2026-06-10T03:00:00Z"),
            java.time.Instant.parse(result.maxServerUpdatedAt!!),
            "the cursor must advance past the unreadable row, not stop before it",
        )
    }

    @Test
    fun `the unreadable row is not written, and the readable ones around it are`() = runTest {
        val page = listOf(
            remoteRow(id = "tx-good", date = READABLE_DATE, serverUpdatedAt = "2026-06-10T01:00:00Z"),
            remoteRow(id = "tx-bad", date = Long.MAX_VALUE, serverUpdatedAt = "2026-06-10T02:00:00Z"),
        )

        pull(page)

        // Not clamped to a sentinel and not written at some invented date: absent. A fabricated
        // occurredAt would corrupt the lexicographic ordering every list and month window uses.
        assertNull(db.transactionsQueries.find("tx-bad").executeAsOneOrNull())
        val good = db.transactionsQueries.find("tx-good").executeAsOneOrNull()
        assertEquals("2025-07-31T17:13:20", good?.occurredAt)
    }

    @Test
    fun `dropping the row says so, and does not blame a foreign key`() = runTest {
        val page = listOf(remoteRow(id = "tx-bad", date = Long.MAX_VALUE, serverUpdatedAt = "2026-06-10T02:00:00Z"))

        pull(page)

        val line = logger.warnings.single()
        assertTrue(line.contains("dropped"), "the line must say the row was dropped: $line")
        assertTrue(line.contains("tx-bad"), "and which row: $line")
        assertFalse(line.contains("fk miss"), "an FK miss is a different, recoverable thing: $line")
    }

    @Test
    fun `a page of nothing but unreadable rows still advances the cursor`() = runTest {
        // The shape that makes the freeze permanent rather than merely slow: nothing lands, so
        // there is nothing whose arrival could ever unstick the window.
        val page = listOf(
            remoteRow(id = "tx-bad-1", date = Long.MAX_VALUE, serverUpdatedAt = "2026-06-10T01:00:00Z"),
            remoteRow(id = "tx-bad-2", date = Long.MIN_VALUE, serverUpdatedAt = "2026-06-10T02:00:00Z"),
        )

        val result = pull(page)

        assertFalse(result.skippedRows)
        assertEquals(
            java.time.Instant.parse("2026-06-10T02:00:00Z"),
            java.time.Instant.parse(result.maxServerUpdatedAt!!),
        )
        assertEquals(0, db.transactionsQueries.all().executeAsList().size)
    }

    // The other half of the distinction — an FK miss must STILL hold the cursor — is pinned in
    // `BaseTableSyncPaginationTest`, not here: the JDBC driver raises `java.sql.SQLException` where
    // the device raises `SQLiteConstraintException`, so this host suite cannot produce a real one.
    // `SyncFkExceptionTest` (instrumented) is what pins that exception type against a real driver.

    private suspend fun pull(page: List<TransactionRowDto>): PullResult =
        PagedTransactionTableSync(db, logger, page).pull(USER, cursor = null, resolver = ConflictResolver())

    private fun remoteRow(id: String, date: Long, serverUpdatedAt: String) = TransactionRowDto(
        transactionId = id,
        type = "Spend",
        amount = 1_000L,
        description = "",
        date = date,
        categoryId = null,
        accountId = "acc-1",
        createdAt = 1L,
        updatedAt = 1L,
        userId = USER,
        deletedAt = null,
        serverUpdatedAt = serverUpdatedAt,
    )

    private companion object {
        const val USER = "user-1"

        /** 2025-07-31 17:13:20 in Lima — the same value the migration test converts. */
        const val READABLE_DATE = 1_754_000_000_000L
    }
}
