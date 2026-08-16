package com.emm.domain.auth

import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
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
    private val backupMetadataStore = mockk<BackupMetadataStore>()
    private val syncMutex = SyncMutex()
    private val logger = mockk<DiagnosticsLogger>(relaxed = true)

    private val useCase = DeleteUserAccountUseCase(
        authRepository = authRepository,
        claimLocalDataRepository = claimLocalDataRepository,
        syncCursorStore = syncCursorStore,
        backupMetadataStore = backupMetadataStore,
        syncMutex = syncMutex,
        logger = logger,
    )

    private fun authenticated(userId: String) =
        SessionStatus.Authenticated(AuthUser(userId = userId, email = "$userId@example.com"))

    @Test
    fun `happy path calls deleteAccount, unclaimAll, cursor clear, then backup metadata clear`() = runTest {
        val userId = "uid-1"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { authRepository.deleteAccount() } just Runs
        coEvery { claimLocalDataRepository.unclaimAll(userId) } just Runs
        every { syncCursorStore.clear(userId) } just Runs
        every { backupMetadataStore.clear(userId) } just Runs

        useCase()

        coVerifyOrder {
            authRepository.deleteAccount()
            claimLocalDataRepository.unclaimAll(userId)
            syncCursorStore.clear(userId)
            backupMetadataStore.clear(userId)
        }
    }

    @Test
    fun `account deletion clears the backup metadata watermark`() = runTest {
        val userId = "uid-backup"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { authRepository.deleteAccount() } just Runs
        coEvery { claimLocalDataRepository.unclaimAll(userId) } just Runs
        every { syncCursorStore.clear(userId) } just Runs
        every { backupMetadataStore.clear(userId) } just Runs

        useCase()

        verify(exactly = 1) { backupMetadataStore.clear(userId) }
    }

    @Test
    fun `NotAuthenticated session throws Unauthorized and makes zero calls to deleteAccount or unclaim`() = runTest {
        every { authRepository.sessionStatus } returns
            flowOf(SessionStatus.Initializing, SessionStatus.NotAuthenticated)

        assertFailsWith<DomainException.Unauthorized> { useCase() }

        coVerify(exactly = 0) { authRepository.deleteAccount() }
        coVerify(exactly = 0) { claimLocalDataRepository.unclaimAll(any()) }
        verify(exactly = 0) { syncCursorStore.clear(any()) }
        verify(exactly = 0) { backupMetadataStore.clear(any()) }
    }

    @Test
    fun `deleteAccount throwing leaves unclaimAll and both clears uncalled`() = runTest {
        val userId = "uid-2"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { authRepository.deleteAccount() } throws
            DomainException.NetworkUnavailable(RuntimeException("timeout"))

        assertFailsWith<DomainException.NetworkUnavailable> { useCase() }

        coVerify(exactly = 0) { claimLocalDataRepository.unclaimAll(any()) }
        verify(exactly = 0) { syncCursorStore.clear(any()) }
        verify(exactly = 0) { backupMetadataStore.clear(any()) }
    }

    @Test
    fun `deleteAccount throwing logs the remote delete step and rethrows the original exception unchanged`() = runTest {
        val userId = "uid-5"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        val original = DomainException.NetworkUnavailable(RuntimeException("timeout"))
        coEvery { authRepository.deleteAccount() } throws original

        val thrown = assertFailsWith<DomainException.NetworkUnavailable> { useCase() }

        assertTrue(thrown === original, "The original exception instance must reach the caller unchanged")
        verify(exactly = 1) { logger.warn(match { it.contains("remote delete") }, original) }
    }

    @Test
    fun `NotAuthenticated session logs the session resolve step and rethrows Unauthorized unchanged`() = runTest {
        every { authRepository.sessionStatus } returns
            flowOf(SessionStatus.Initializing, SessionStatus.NotAuthenticated)

        val thrown = assertFailsWith<DomainException.Unauthorized> { useCase() }

        verify(exactly = 1) { logger.warn(match { it.contains("session resolve") }, thrown) }
    }

    @Test
    fun `cancelling the caller during the remote delete step is not logged as a failure`() = runTest {
        val userId = "uid-6"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        val gate = CompletableDeferred<Unit>()
        coEvery { authRepository.deleteAccount() } coAnswers { gate.await() }

        val job = launch { useCase() }
        testScheduler.advanceUntilIdle()
        job.cancelAndJoin()

        verify(exactly = 0) { logger.warn(any(), any()) }
    }

    @Test
    fun `cancellation arriving after deleteAccount returns still runs unclaimAll and both clears`() = runTest {
        val userId = "uid-7"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { authRepository.deleteAccount() } just Runs
        val unclaimGate = CompletableDeferred<Unit>()
        coEvery { claimLocalDataRepository.unclaimAll(userId) } coAnswers { unclaimGate.await() }
        every { syncCursorStore.clear(userId) } just Runs
        every { backupMetadataStore.clear(userId) } just Runs

        val job = launch { useCase() }
        testScheduler.advanceUntilIdle()

        job.cancel()
        testScheduler.advanceUntilIdle()

        verify(exactly = 0) { syncCursorStore.clear(userId) }
        verify(exactly = 0) { backupMetadataStore.clear(userId) }

        unclaimGate.complete(Unit)
        job.join()

        coVerify(exactly = 1) { claimLocalDataRepository.unclaimAll(userId) }
        verify(exactly = 1) { syncCursorStore.clear(userId) }
        verify(exactly = 1) { backupMetadataStore.clear(userId) }
    }

    @Test
    fun `Initializing followed by Authenticated resolves correctly and proceeds`() = runTest {
        val userId = "uid-3"
        every { authRepository.sessionStatus } returns
            flowOf(SessionStatus.Initializing, authenticated(userId))
        coEvery { authRepository.deleteAccount() } just Runs
        coEvery { claimLocalDataRepository.unclaimAll(userId) } just Runs
        every { syncCursorStore.clear(userId) } just Runs
        every { backupMetadataStore.clear(userId) } just Runs

        useCase()

        coVerify(exactly = 1) { authRepository.deleteAccount() }
        coVerify(exactly = 1) { claimLocalDataRepository.unclaimAll(userId) }
        verify(exactly = 1) { syncCursorStore.clear(userId) }
        verify(exactly = 1) { backupMetadataStore.clear(userId) }
    }

    @Test
    fun `deletion waits for an in-flight holder of the shared SyncMutex`() = runTest {
        val userId = "uid-4"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { authRepository.deleteAccount() } just Runs
        coEvery { claimLocalDataRepository.unclaimAll(userId) } just Runs
        every { syncCursorStore.clear(userId) } just Runs
        every { backupMetadataStore.clear(userId) } just Runs

        val syncGate = CompletableDeferred<Unit>()
        val syncJob = launch { syncMutex.withLock { syncGate.await() } }
        val deleteJob = launch { useCase() }
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { authRepository.deleteAccount() }

        syncGate.complete(Unit)
        testScheduler.advanceUntilIdle()
        syncJob.join()
        deleteJob.join()

        coVerify(exactly = 1) { authRepository.deleteAccount() }
        coVerify(exactly = 1) { claimLocalDataRepository.unclaimAll(userId) }
    }

    @Test
    fun `a session stuck in Initializing fails with NetworkUnavailable and releases the sync mutex`() = runTest {
        every { authRepository.sessionStatus } returns MutableStateFlow(SessionStatus.Initializing)

        assertFailsWith<DomainException.NetworkUnavailable> { useCase() }

        coVerify(exactly = 0) { authRepository.deleteAccount() }
        coVerify(exactly = 0) { claimLocalDataRepository.unclaimAll(any()) }
        verify(exactly = 0) { syncCursorStore.clear(any()) }
        verify(exactly = 0) { backupMetadataStore.clear(any()) }

        assertTrue(syncMutex.withLock { true }, "The shared SyncMutex must be free again")
    }

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
        testScheduler.runCurrent()
        job.cancelAndJoin()

        assertNull(domainFailure, "Cancellation must propagate as cancellation, got $domainFailure")
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }
}
