package com.emm.data.sync

import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.ConflictResolver
import com.emm.domain.sync.SyncCursorStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [DefaultSyncRepository].
 *
 * The four [TableSync] slots are MockK mocks of the [TableSync] interface.
 * Call-order tracking is done by a shared [callLog] list that each mock appends to via
 * `coAnswers`. The session/auth dependency is driven by an in-line [AuthRepository] object.
 */
class DefaultSyncRepositoryTest {

    // ---------------------------------------------------------------------------
    // In-memory cursor store (pure fake, no mocking)
    // ---------------------------------------------------------------------------

    private class FakeSyncCursorStore : SyncCursorStore {
        private val cursors = mutableMapOf<String, String>()

        override fun lastPulledAt(userId: String): String? = cursors[userId]

        override fun setLastPulledAt(userId: String, cursor: String) {
            cursors[userId] = cursor
        }
    }

    // ---------------------------------------------------------------------------
    // Test fixtures
    // ---------------------------------------------------------------------------

    private val callLog = mutableListOf<String>()
    private val cursorStore = FakeSyncCursorStore()
    private val conflictResolver = ConflictResolver()

    private val accountSync: TableSync = mockk(relaxed = true)
    private val categorySync: TableSync = mockk(relaxed = true)
    private val transactionSync: TableSync = mockk(relaxed = true)
    private val recurringSync: TableSync = mockk(relaxed = true)

    private lateinit var repository: DefaultSyncRepository

    private fun buildRepository(authRepo: AuthRepository): DefaultSyncRepository {
        val observeSession = ObserveSessionUseCase(authRepo)
        return DefaultSyncRepository(
            observeSession = observeSession,
            cursorStore = cursorStore,
            conflictResolver = conflictResolver,
            accountSync = accountSync,
            categorySync = categorySync,
            transactionSync = transactionSync,
            recurringSync = recurringSync,
        )
    }

    private fun authRepoWith(status: SessionStatus): AuthRepository = object : AuthRepository {
        override val sessionStatus = flowOf(status)
        override suspend fun signIn(email: String, password: String) = error("not used")
        override suspend fun signUp(email: String, password: String) = null
        override suspend fun signOut() = Unit
    }

    private fun authenticatedUser() = SessionStatus.Authenticated(AuthUser(userId = "user-1", email = "a@b.com"))

    @Before
    fun setUp() {
        repository = buildRepository(authRepoWith(authenticatedUser()))

        // Record push calls.
        coEvery { accountSync.push(any()) } coAnswers { callLog += "push:accounts" }
        coEvery { categorySync.push(any()) } coAnswers { callLog += "push:categories" }
        coEvery { transactionSync.push(any()) } coAnswers { callLog += "push:transactions" }
        coEvery { recurringSync.push(any()) } coAnswers { callLog += "push:recurring" }

        // Record pull calls (while still returning the default result).
        coEvery { accountSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:accounts"
            PullResult(null, false)
        }
        coEvery { categorySync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:categories"
            PullResult(null, false)
        }
        coEvery { transactionSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:transactions"
            PullResult(null, false)
        }
        coEvery { recurringSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:recurring"
            PullResult(null, false)
        }
    }

    // ---------------------------------------------------------------------------
    // Call order: push all 4 → pull all 4, in FK-safe order
    // ---------------------------------------------------------------------------

    @Test
    fun `sync follows FK-safe order accounts then categories then transactions then recurring`() = runTest {
        repository.sync()

        val expected = listOf(
            "push:accounts",
            "push:categories",
            "push:transactions",
            "push:recurring",
            "pull:accounts",
            "pull:categories",
            "pull:transactions",
            "pull:recurring",
        )
        assertEquals(expected, callLog)
    }

    @Test
    fun `sync pushes all four tables before any pull`() = runTest {
        repository.sync()

        val lastPushIndex = callLog.indexOfLast { it.startsWith("push:") }
        val firstPullIndex = callLog.indexOfFirst { it.startsWith("pull:") }
        assertTrue(lastPushIndex < firstPullIndex, "All pushes must happen before any pull")
    }

    // ---------------------------------------------------------------------------
    // Cursor advancement: max of all maxServerUpdatedAt compared as Instant
    // ---------------------------------------------------------------------------

    @Test
    fun `pull advances cursor to max server_updated_at across all four tables`() = runTest {
        coEvery { accountSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:accounts"
            PullResult("2026-06-10T01:00:00Z", false)
        }
        coEvery { categorySync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:categories"
            PullResult("2026-06-10T02:00:00Z", false)
        }
        coEvery { transactionSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:transactions"
            PullResult("2026-06-10T03:00:00Z", false)
        }
        coEvery { recurringSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:recurring"
            PullResult("2026-06-10T01:30:00Z", false)
        }

        repository.sync()

        assertEquals("2026-06-10T03:00:00Z", cursorStore.lastPulledAt("user-1"))
    }

    @Test
    fun `pull picks max by Instant not lexicographic string comparison`() = runTest {
        // 02:00:00.999999Z sorts lexicographically after 02:00:01Z but is chronologically earlier.
        coEvery { accountSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:accounts"
            PullResult("2026-06-10T02:00:00.999999Z", false)
        }
        coEvery { categorySync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:categories"
            PullResult("2026-06-10T02:00:01Z", false)
        }

        repository.sync()

        // 02:00:01Z is after 02:00:00.999999Z — must be chosen as the max.
        assertEquals("2026-06-10T02:00:01Z", cursorStore.lastPulledAt("user-1"))
    }

    @Test
    fun `pull uses stored cursor when calling each table`() = runTest {
        cursorStore.setLastPulledAt("user-1", "2026-06-01T00:00:00Z")

        repository.sync()

        coVerify { accountSync.pull("user-1", "2026-06-01T00:00:00Z", any()) }
        coVerify { categorySync.pull("user-1", "2026-06-01T00:00:00Z", any()) }
    }

    // ---------------------------------------------------------------------------
    // skippedRows → cursor NOT advanced
    // ---------------------------------------------------------------------------

    @Test
    fun `cursor not advanced when any table reports skippedRows`() = runTest {
        cursorStore.setLastPulledAt("user-1", "2026-06-01T00:00:00Z")
        coEvery { accountSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:accounts"
            PullResult("2026-06-10T01:00:00Z", false)
        }
        coEvery { transactionSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:transactions"
            PullResult("2026-06-10T01:00:00Z", skippedRows = true)
        }

        repository.sync()

        assertEquals("2026-06-01T00:00:00Z", cursorStore.lastPulledAt("user-1"))
    }

    @Test
    fun `cursor not advanced when all tables report skippedRows`() = runTest {
        cursorStore.setLastPulledAt("user-1", "2026-05-01T00:00:00Z")
        coEvery { accountSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:accounts"
            PullResult("2026-06-10T01:00:00Z", skippedRows = true)
        }
        coEvery { categorySync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:categories"
            PullResult("2026-06-10T01:00:00Z", skippedRows = true)
        }
        coEvery { transactionSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:transactions"
            PullResult("2026-06-10T01:00:00Z", skippedRows = true)
        }
        coEvery { recurringSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:recurring"
            PullResult("2026-06-10T01:00:00Z", skippedRows = true)
        }

        repository.sync()

        assertEquals("2026-05-01T00:00:00Z", cursorStore.lastPulledAt("user-1"))
    }

    // ---------------------------------------------------------------------------
    // All tables return null → cursor unchanged
    // ---------------------------------------------------------------------------

    @Test
    fun `cursor unchanged when all tables return null maxServerUpdatedAt`() = runTest {
        cursorStore.setLastPulledAt("user-1", "2026-06-01T00:00:00Z")

        repository.sync()

        assertEquals("2026-06-01T00:00:00Z", cursorStore.lastPulledAt("user-1"))
    }

    @Test
    fun `cursor stays null when no prior cursor and all tables return null`() = runTest {
        repository.sync()

        assertNull(cursorStore.lastPulledAt("user-1"))
    }

    // ---------------------------------------------------------------------------
    // Push throws DomainException → no pull, cursor unchanged, exception propagates
    // ---------------------------------------------------------------------------

    @Test
    fun `push failure aborts cycle — no pull is called`() = runTest {
        cursorStore.setLastPulledAt("user-1", "2026-06-01T00:00:00Z")
        val pushError = DomainException.NetworkUnavailable(RuntimeException("timeout"))
        coEvery { accountSync.push(any()) } coAnswers {
            callLog += "push:accounts"
            throw pushError
        }

        assertFailsWith<DomainException.NetworkUnavailable> {
            repository.sync()
        }

        assertTrue(callLog.none { it.startsWith("pull:") }, "No pull should fire after push failure")
    }

    @Test
    fun `push failure leaves cursor unchanged`() = runTest {
        cursorStore.setLastPulledAt("user-1", "2026-06-01T00:00:00Z")
        coEvery { accountSync.push(any()) } coAnswers {
            callLog += "push:accounts"
            throw DomainException.DatabaseError(RuntimeException("db error"))
        }

        runCatching { repository.sync() }

        assertEquals("2026-06-01T00:00:00Z", cursorStore.lastPulledAt("user-1"))
    }

    @Test
    fun `push failure with DomainException propagates the original exception type`() = runTest {
        val original = DomainException.Unauthorized("401")
        coEvery { categorySync.push(any()) } coAnswers {
            callLog += "push:categories"
            throw original
        }

        val thrown = assertFailsWith<DomainException.Unauthorized> {
            repository.sync()
        }
        assertEquals(original, thrown)
    }

    // ---------------------------------------------------------------------------
    // Session stuck in Initializing → DomainException.NetworkUnavailable via timeout
    // ---------------------------------------------------------------------------

    @Test
    fun `initializing session that never resolves throws NetworkUnavailable after timeout`() = runTest {
        // The flow emits Initializing then suspends forever via awaitCancellation().
        // withTimeout(10_000ms) fires immediately under runTest virtual time.
        val blockingAuthRepo = object : AuthRepository {
            override val sessionStatus = flow {
                emit(SessionStatus.Initializing)
                awaitCancellation()
            }
            override suspend fun signIn(email: String, password: String) = error("not used")
            override suspend fun signUp(email: String, password: String) = null
            override suspend fun signOut() = Unit
        }
        val repo = buildRepository(blockingAuthRepo)

        assertFailsWith<DomainException.NetworkUnavailable> {
            repo.sync()
        }
    }

    // ---------------------------------------------------------------------------
    // Not-authenticated session → no-op (no push or pull, no exception)
    // ---------------------------------------------------------------------------

    @Test
    fun `not-authenticated session is a no-op — no push or pull called`() = runTest {
        val repo = buildRepository(authRepoWith(SessionStatus.NotAuthenticated))

        repo.sync()

        coVerify(exactly = 0) { accountSync.push(any()) }
        coVerify(exactly = 0) { accountSync.pull(any(), any(), any()) }
    }

    @Test
    fun `not-authenticated session leaves cursor unchanged`() = runTest {
        cursorStore.setLastPulledAt("user-1", "2026-06-01T00:00:00Z")
        val repo = buildRepository(authRepoWith(SessionStatus.NotAuthenticated))

        repo.sync()

        assertEquals("2026-06-01T00:00:00Z", cursorStore.lastPulledAt("user-1"))
    }

    // ---------------------------------------------------------------------------
    // userId from session is passed to table syncs
    // ---------------------------------------------------------------------------

    @Test
    fun `sync keys cursor store by userId from authenticated session`() = runTest {
        val authRepo = object : AuthRepository {
            override val sessionStatus = flowOf(
                SessionStatus.Authenticated(AuthUser(userId = "specific-user", email = null)),
            )
            override suspend fun signIn(email: String, password: String) = error("not used")
            override suspend fun signUp(email: String, password: String) = null
            override suspend fun signOut() = Unit
        }
        coEvery { accountSync.pull(any(), any(), any()) } coAnswers {
            callLog += "pull:accounts"
            PullResult("2026-06-10T01:00:00Z", skippedRows = false)
        }
        val repo = buildRepository(authRepo)

        repo.sync()

        assertNull(cursorStore.lastPulledAt("user-1"), "Should not store under the wrong userId")
        assertEquals("2026-06-10T01:00:00Z", cursorStore.lastPulledAt("specific-user"))
    }
}
