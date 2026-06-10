package com.emm.data.sync

import com.emm.domain.sync.ConflictResolver
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * JVM unit tests for the composite-keyset pagination loop in [BaseTableSync.pull].
 *
 * Uses a concrete [FakeTableSync] backed by a pluggable [RemotePageFetcher] that can be either
 * a real keyset-aware in-memory store ([KeysetStore]) or a custom lambda for edge-case tests.
 * No Supabase client, no Android runtime required.
 */
class BaseTableSyncPaginationTest {

    // -------------------------------------------------------------------------
    // In-memory DTO for tests
    // -------------------------------------------------------------------------

    data class TestDto(
        override val userId: String,
        override val updatedAt: Long,
        override val serverUpdatedAt: String?,
        val id: String,
    ) : SyncRowDto

    // -------------------------------------------------------------------------
    // Remote page fetcher abstraction
    // -------------------------------------------------------------------------

    fun interface RemotePageFetcher {
        fun fetchPage(userId: String, overlapCursor: String?, after: PullPageKey?, limit: Int): List<TestDto>
    }

    /**
     * Simulates a PostgREST table with composite keyset filter + ordering.
     *
     * Rows are pre-sorted by (serverUpdatedAt ASC, id ASC) matching the ORDER BY applied in
     * production. [fetchPage] replicates the composite keyset filter exactly.
     */
    class KeysetStore(rows: List<TestDto>) : RemotePageFetcher {
        private val sortedRows: List<TestDto> = rows.sortedWith(
            compareBy({ it.serverUpdatedAt ?: "" }, { it.id }),
        )

        override fun fetchPage(
            userId: String,
            overlapCursor: String?,
            after: PullPageKey?,
            limit: Int,
        ): List<TestDto> = sortedRows
            .filter { it.userId == userId }
            .filter { row ->
                val sat = row.serverUpdatedAt ?: return@filter false
                when {
                    after != null ->
                        // Composite keyset: (sat, id) strictly dominates `after`
                        sat > after.serverUpdatedAt ||
                            (sat == after.serverUpdatedAt && row.id > after.pk)

                    overlapCursor != null -> sat >= overlapCursor

                    else -> true
                }
            }
            .take(limit)
    }

    // -------------------------------------------------------------------------
    // Concrete BaseTableSync subclass for tests
    // -------------------------------------------------------------------------

    /**
     * [BaseTableSync] with no Android/Supabase dependencies.
     *
     * [transact] is a plain pass-through: `{ body -> body() }`.
     * [fetchRemotePage] delegates to a [RemotePageFetcher].
     */
    inner class FakeTableSync(private val fetcher: RemotePageFetcher, pageSize: Int = DEFAULT_TEST_PAGE_SIZE) :
        BaseTableSync<TestDto>(
            client = mockk(relaxed = true),
            transact = { body -> body() },
            pageSize = pageSize,
        ) {
        override val pkColumn: String = "id"

        val applied = mutableListOf<TestDto>()
        val resynced = mutableListOf<String>()

        /** Pre-populate to make the resolver return KeepLocal for a given pk. */
        val localTimestamps = mutableMapOf<String, Long>()

        /** Ids that [applyRemoteRow] should reject (FK miss simulation). */
        val failIds = mutableSetOf<String>()

        override suspend fun fetchRemotePage(
            userId: String,
            overlapCursor: String?,
            after: PullPageKey?,
            limit: Int,
        ): List<TestDto> = fetcher.fetchPage(userId, overlapCursor, after, limit)

        override fun applyRemoteRow(remote: TestDto): Boolean {
            if (remote.id in failIds) return false
            applied += remote
            return true
        }

        override fun localUpdatedAt(pk: String): Long? = localTimestamps[pk]

        override fun markPendingForResync(pk: String) {
            resynced += pk
        }

        // Push hooks — not exercised in pull tests
        override suspend fun selectPendingDtos(userId: String): List<TestDto> = emptyList()
        override fun pkOf(dto: TestDto): String = dto.id
        override fun markSynced(pk: String, updatedAt: Long) = Unit
        override suspend fun upsertDtos(dtos: List<TestDto>) = Unit
        override fun pendingCount(): Flow<Long> = flowOf(0L)
    }

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    private val userId = "user-1"
    private val resolver = ConflictResolver()

    private fun dto(id: String, sat: String, uid: String = userId, updatedAt: Long = 100L) =
        TestDto(userId = uid, updatedAt = updatedAt, serverUpdatedAt = sat, id = id)

    private fun store(vararg rows: TestDto) = KeysetStore(rows.toList())

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `empty first page returns null maxServerUpdatedAt and skippedRows false`() = runTest {
        val sync = FakeTableSync(store())

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertNull(result.maxServerUpdatedAt)
        assertFalse(result.skippedRows)
        assertEquals(emptyList(), sync.applied)
    }

    @Test
    fun `single page smaller than limit applies all rows and returns correct max`() = runTest {
        val sync = FakeTableSync(
            store(dto("a", "2026-01-01T00:00:01Z"), dto("b", "2026-01-01T00:00:02Z")),
            pageSize = 5,
        )

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertEquals(listOf("a", "b"), sync.applied.map { it.id })
        assertFalse(result.skippedRows)
        val actual = java.time.Instant.parse(result.maxServerUpdatedAt!!)
        assertEquals(java.time.Instant.parse("2026-01-01T00:00:02Z"), actual)
    }

    @Test
    fun `multi-page pull applies ALL rows exactly once`() = runTest {
        // 7 rows, page size 3 → pages of size [3, 3, 1]
        val rows = (1..7).map { i -> dto("id$i", "2026-01-01T00:00:0${i}Z") }
        val sync = FakeTableSync(KeysetStore(rows), pageSize = 3)

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertEquals(7, sync.applied.size)
        assertEquals((1..7).map { "id$it" }, sync.applied.map { it.id })
        assertFalse(result.skippedRows)
        val actual = java.time.Instant.parse(result.maxServerUpdatedAt!!)
        assertEquals(java.time.Instant.parse("2026-01-01T00:00:07Z"), actual)
    }

    /**
     * Core regression pin: a batch of rows sharing ONE server_updated_at (because Postgres
     * `now()` is transaction-stable) must be fully applied even when the batch is larger than
     * the page size. Timestamp-only pagination (gt) would drop rows at the boundary;
     * composite keyset (serverUpdatedAt, pk) sees them all.
     */
    @Test
    fun `same-timestamp batch larger than page size is fully applied without infinite loop`() = runTest {
        val sharedSat = "2026-06-10T12:00:00Z"
        // 5 rows with identical sat; sorted sub-key is id ("aa" < "bb" < … < "ee")
        val rows = listOf("aa", "bb", "cc", "dd", "ee").map { id -> dto(id, sharedSat) }
        val sync = FakeTableSync(KeysetStore(rows), pageSize = 2)

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertEquals(5, sync.applied.size)
        assertEquals(listOf("aa", "bb", "cc", "dd", "ee"), sync.applied.map { it.id })
        assertFalse(result.skippedRows)
    }

    @Test
    fun `page boundary tiebreak rows with equal serverUpdatedAt paginate without loss or duplication`() = runTest {
        val t1 = "2026-06-10T10:00:00Z"
        val t2 = "2026-06-10T10:00:01Z"
        val rows = listOf(
            dto("a1", t1),
            dto("a2", t1),
            dto("a3", t1),
            dto("b1", t2),
            dto("b2", t2),
            dto("b3", t2),
        )
        val sync = FakeTableSync(KeysetStore(rows), pageSize = 2)

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertEquals(6, sync.applied.size)
        // Check no duplicates and all present
        assertEquals(
            setOf("a1", "a2", "a3", "b1", "b2", "b3"),
            sync.applied.map { it.id }.toSet(),
        )
        assertFalse(result.skippedRows)
        assertEquals(java.time.Instant.parse(t2), java.time.Instant.parse(result.maxServerUpdatedAt!!))
    }

    @Test
    fun `skipped from FK-miss on page 1 stays true even when later pages are clean`() = runTest {
        val rows = listOf(
            dto("a", "2026-01-01T00:00:01Z"), // FK miss
            dto("b", "2026-01-01T00:00:02Z"),
            dto("c", "2026-01-01T00:00:03Z"),
            dto("d", "2026-01-01T00:00:04Z"),
        )
        val sync = FakeTableSync(KeysetStore(rows), pageSize = 2)
        sync.failIds += "a"

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertTrue(result.skippedRows, "skippedRows must stay true from page-1 FK miss")
        assertEquals(listOf("b", "c", "d"), sync.applied.map { it.id })
    }

    @Test
    fun `MAX_PULL_PAGES guard terminates and sets skippedRows when server always returns full pages`() = runTest {
        val pageSize = 3
        var callCount = 0

        // Generates infinite unique rows with always-advancing keys
        val infiniteFetcher = RemotePageFetcher { uid, _, after, limit ->
            val startId = callCount * limit
            callCount++
            (0 until limit).map { i ->
                val n = startId + i
                TestDto(
                    userId = uid,
                    updatedAt = 100L,
                    // Use padded numbers: lexicographic order == chronological order
                    serverUpdatedAt = "2026-01-01T%02d:%02d:%02dZ".format(n / 3600, (n / 60) % 60, n % 60),
                    id = "id%08d".format(n),
                )
            }
        }
        val sync = FakeTableSync(infiniteFetcher, pageSize = pageSize)

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertTrue(result.skippedRows, "MAX_PULL_PAGES guard must set skippedRows=true")
        assertEquals(BaseTableSync.MAX_PULL_PAGES * pageSize, sync.applied.size)
        assertEquals(BaseTableSync.MAX_PULL_PAGES, callCount)
    }

    /**
     * Regression pin for the specific broken-keyset scenario the MAX_PULL_PAGES guard defends
     * against: a server (or test double) that ignores the `after` key and always returns the
     * same full page regardless of the cursor position.
     *
     * Unlike the infinite-unique-rows test above, here the keyset NEVER advances (the page
     * content is always identical). The loop must still terminate after MAX_PULL_PAGES calls,
     * report skippedRows = true, and not hang.
     */
    @Test
    fun `stuck keyset — server always returns same page — terminates after MAX_PULL_PAGES`() = runTest {
        val pageSize = 2
        var callCount = 0

        // Returns the same two rows on every call, ignoring the `after` key entirely.
        val stuckFetcher = RemotePageFetcher { uid, _, _, _ ->
            callCount++
            listOf(
                TestDto(userId = uid, updatedAt = 100L, serverUpdatedAt = "2026-01-01T00:00:01Z", id = "id-1"),
                TestDto(userId = uid, updatedAt = 100L, serverUpdatedAt = "2026-01-01T00:00:02Z", id = "id-2"),
            )
        }
        val sync = FakeTableSync(stuckFetcher, pageSize = pageSize)

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertEquals(BaseTableSync.MAX_PULL_PAGES, callCount, "fetch must be called exactly MAX_PULL_PAGES times")
        assertTrue(result.skippedRows, "stuck keyset must set skippedRows=true so cursor is held")
    }

    /**
     * Off-by-one pin: a pull whose LAST page lands exactly on the MAX_PULL_PAGES budget and
     * stops cleanly (partial page) must NOT be treated as a broken keyset — skippedRows stays
     * false so the cursor advances normally.
     */
    @Test
    fun `pull completing cleanly on exactly the last budgeted page does not hold the cursor`() = runTest {
        val pageSize = 2
        // (MAX_PULL_PAGES - 1) full pages + 1 partial page = clean stop on page MAX_PULL_PAGES.
        val rowCount = (BaseTableSync.MAX_PULL_PAGES - 1) * pageSize + 1
        val rows = (0 until rowCount).map { i ->
            dto(
                id = "id%04d".format(i),
                sat = "2026-01-01T%02d:%02d:%02dZ".format(i / 3600, (i / 60) % 60, i % 60),
            )
        }
        val sync = FakeTableSync(KeysetStore(rows), pageSize = pageSize)

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertEquals(rowCount, sync.applied.size)
        assertFalse(result.skippedRows, "clean stop on the last budgeted page must not hold the cursor")
    }

    @Test
    fun `userId mismatch row is not applied and sets skippedRows true`() = runTest {
        // Bypasses the server-side userId filter to simulate a buggy server / RLS misconfiguration:
        // the defence-in-depth check in applyPage must catch this and set skippedRows.
        val rowA = dto("a", "2026-01-01T00:00:01Z", uid = "other-user")
        val rowB = dto("b", "2026-01-01T00:00:02Z")
        val sync = FakeTableSync(
            fetcher = RemotePageFetcher { _, _, _, _ -> listOf(rowA, rowB) },
            pageSize = 5,
        )

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertTrue(result.skippedRows)
        assertEquals(listOf("b"), sync.applied.map { it.id })
    }

    @Test
    fun `userId mismatch row on page 2 sets skippedRows true`() = runTest {
        // Page 1: [a, b] (clean). Page 2: [c (evil), d]. Using a stateful fetcher that
        // returns mismatched rows only on the second call.
        val rowA = dto("a", "2026-01-01T00:00:01Z")
        val rowB = dto("b", "2026-01-01T00:00:02Z")
        val rowC = dto("c", "2026-01-01T00:00:03Z", uid = "evil-user")
        val rowD = dto("d", "2026-01-01T00:00:04Z")
        var call = 0
        val sync = FakeTableSync(
            fetcher = RemotePageFetcher { _, _, _, _ ->
                when (call++) {
                    0 -> listOf(rowA, rowB)
                    1 -> listOf(rowC, rowD)
                    else -> emptyList()
                }
            },
            pageSize = 2,
        )

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertTrue(result.skippedRows)
        assertEquals(listOf("a", "b", "d"), sync.applied.map { it.id })
    }

    @Test
    fun `overlapCursor is applied on first page and matching rows are included`() = runTest {
        // cursor = T+10s → overlapCursor = T+0s (OVERLAP_SECONDS = 10L subtracted, see SyncCursorUtils)
        // If OVERLAP_SECONDS ever changes, the cursor value "2026-01-01T00:00:10Z" and the
        // "old" sat "2026-01-01T00:00:00Z" must be kept exactly OVERLAP_SECONDS apart so that
        // "old" == overlapCursor and is included by the gte boundary.
        // "new" is after the cursor → also included.
        val rows = listOf(
            dto("old", "2026-01-01T00:00:00Z"),
            dto("new", "2026-01-01T00:01:00Z"),
        )
        val sync = FakeTableSync(KeysetStore(rows), pageSize = 5)

        val result = sync.pull(userId, cursor = "2026-01-01T00:00:10Z", resolver = resolver)

        assertEquals(2, sync.applied.size)
        assertFalse(result.skippedRows)
    }

    @Test
    fun `maxServerUpdatedAt equals global max across all pages`() = runTest {
        val rows = listOf(
            dto("a", "2026-01-01T00:00:01Z"),
            dto("b", "2026-01-01T00:00:05Z"),
            dto("c", "2026-01-01T00:00:03Z"),
            dto("d", "2026-01-01T00:00:09Z"),
            dto("e", "2026-01-01T00:00:02Z"),
        )
        val sync = FakeTableSync(KeysetStore(rows), pageSize = 2)

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertEquals(
            java.time.Instant.parse("2026-01-01T00:00:09Z"),
            java.time.Instant.parse(result.maxServerUpdatedAt!!),
        )
    }

    @Test
    fun `KeepLocal rows are marked for resync and excluded from applied`() = runTest {
        val rows = listOf(
            dto("a", "2026-01-01T00:00:01Z", updatedAt = 100L),
            dto("b", "2026-01-01T00:00:02Z", updatedAt = 100L),
        )
        val sync = FakeTableSync(KeysetStore(rows), pageSize = 5)
        // local "a" is newer → KeepLocal
        sync.localTimestamps["a"] = 999L

        val result = sync.pull(userId, cursor = null, resolver = resolver)

        assertEquals(listOf("b"), sync.applied.map { it.id })
        assertEquals(listOf("a"), sync.resynced)
        assertFalse(result.skippedRows)
    }

    companion object {
        private const val DEFAULT_TEST_PAGE_SIZE = 3
    }
}
