package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.SyncCursorStore
import com.emm.domain.sync.SyncMutex
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeleteUserAccountUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val claimLocalDataRepository = mockk<ClaimLocalDataRepository>()
    private val syncCursorStore = mockk<SyncCursorStore>()
    private val syncMutex = SyncMutex()

    private val useCase = DeleteUserAccountUseCase(
        authRepository = authRepository,
        claimLocalDataRepository = claimLocalDataRepository,
        syncCursorStore = syncCursorStore,
        syncMutex = syncMutex,
    )

    private fun authenticated(userId: String) =
        SessionStatus.Authenticated(AuthUser(userId = userId, email = "$userId@example.com"))

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `happy path calls deleteAccount then unclaimAll then clear in order`() = runTest {
        val userId = "uid-1"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { authRepository.deleteAccount() } just Runs
        coEvery { claimLocalDataRepository.unclaimAll(userId) } just Runs
        every { syncCursorStore.clear(userId) } just Runs

        useCase()

        coVerifyOrder {
            authRepository.deleteAccount()
            claimLocalDataRepository.unclaimAll(userId)
            syncCursorStore.clear(userId)
        }
    }

    // ── Not authenticated ─────────────────────────────────────────────────────

    @Test
    fun `NotAuthenticated session throws Unauthorized and makes zero calls to deleteAccount or unclaim`() = runTest {
        every { authRepository.sessionStatus } returns
            flowOf(SessionStatus.Initializing, SessionStatus.NotAuthenticated)

        assertFailsWith<DomainException.Unauthorized> { useCase() }

        coVerify(exactly = 0) { authRepository.deleteAccount() }
        coVerify(exactly = 0) { claimLocalDataRepository.unclaimAll(any()) }
        verify(exactly = 0) { syncCursorStore.clear(any()) }
    }

    // ── deleteAccount throws ──────────────────────────────────────────────────

    @Test
    fun `deleteAccount throwing leaves unclaimAll and clear uncalled`() = runTest {
        val userId = "uid-2"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { authRepository.deleteAccount() } throws
            DomainException.NetworkUnavailable(RuntimeException("timeout"))

        assertFailsWith<DomainException.NetworkUnavailable> { useCase() }

        coVerify(exactly = 0) { claimLocalDataRepository.unclaimAll(any()) }
        verify(exactly = 0) { syncCursorStore.clear(any()) }
    }

    // ── Initializing then Authenticated ──────────────────────────────────────

    @Test
    fun `Initializing followed by Authenticated resolves correctly and proceeds`() = runTest {
        val userId = "uid-3"
        every { authRepository.sessionStatus } returns
            flowOf(SessionStatus.Initializing, authenticated(userId))
        coEvery { authRepository.deleteAccount() } just Runs
        coEvery { claimLocalDataRepository.unclaimAll(userId) } just Runs
        every { syncCursorStore.clear(userId) } just Runs

        useCase()

        coVerify(exactly = 1) { authRepository.deleteAccount() }
        coVerify(exactly = 1) { claimLocalDataRepository.unclaimAll(userId) }
        verify(exactly = 1) { syncCursorStore.clear(userId) }
    }

    // ── SyncMutex serialization ───────────────────────────────────────────────

    @Test
    fun `deletion waits for an in-flight holder of the shared SyncMutex`() = runTest {
        val userId = "uid-4"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { authRepository.deleteAccount() } just Runs
        coEvery { claimLocalDataRepository.unclaimAll(userId) } just Runs
        every { syncCursorStore.clear(userId) } just Runs

        // Simulate an in-flight sync cycle holding the shared lock.
        val syncGate = CompletableDeferred<Unit>()
        val syncJob = launch { syncMutex.withLock { syncGate.await() } }
        val deleteJob = launch { useCase() }
        testScheduler.advanceUntilIdle()

        // Lock still held: the deletion must not have reached the remote RPC.
        coVerify(exactly = 0) { authRepository.deleteAccount() }

        syncGate.complete(Unit)
        testScheduler.advanceUntilIdle()
        syncJob.join()
        deleteJob.join()

        coVerify(exactly = 1) { authRepository.deleteAccount() }
        coVerify(exactly = 1) { claimLocalDataRepository.unclaimAll(userId) }
    }

    // ── Bounded session resolution ────────────────────────────────────────────

    /**
     * A session that never leaves [SessionStatus.Initializing] used to park the collector forever
     * WHILE HOLDING the shared [SyncMutex]: the UI stayed on its "deleting" state and every sync
     * cycle was blocked for the rest of the process lifetime. The wait is now bounded.
     */
    @Test
    fun `a session stuck in Initializing fails with NetworkUnavailable and releases the sync mutex`() = runTest {
        every { authRepository.sessionStatus } returns MutableStateFlow(SessionStatus.Initializing)

        assertFailsWith<DomainException.NetworkUnavailable> { useCase() }

        coVerify(exactly = 0) { authRepository.deleteAccount() }
        coVerify(exactly = 0) { claimLocalDataRepository.unclaimAll(any()) }
        verify(exactly = 0) { syncCursorStore.clear(any()) }

        // The point of the bound: sync must be able to run again after a stuck session.
        assertTrue(syncMutex.withLock { true }, "The shared SyncMutex must be free again")
    }

    /**
     * `TimeoutCancellationException` IS a `CancellationException`, so the timeout branch must catch
     * that exact type. A genuine cancellation of the caller has to stay a cancellation — converting
     * it would report a failed deletion for a screen the user simply left.
     */
    @Test
    fun `cancelling the caller while the session is still resolving does not surface as a DomainException`() = runTest {
        every { authRepository.sessionStatus } returns MutableStateFlow(SessionStatus.Initializing)
        var domainFailure: DomainException? = null

        val job = launch {
            try {
                useCase()
            } catch (e: DomainException) {
                domainFailure = e
            }
        }
        // runCurrent, NOT advanceUntilIdle: parks the caller inside the collector without letting
        // virtual time reach the timeout, so the cancellation below is the only thing under test.
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertNull(domainFailure, "Cancellation must propagate as cancellation, got $domainFailure")
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }
}
