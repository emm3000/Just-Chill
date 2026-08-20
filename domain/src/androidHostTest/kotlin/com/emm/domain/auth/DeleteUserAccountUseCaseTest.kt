package com.emm.domain.auth

import com.emm.domain.shared.RemoteWriteMutex
import com.emm.domain.shared.backup.BackupEraser
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class DeleteUserAccountUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val backupEraser = mockk<BackupEraser>()
    private val backupMetadataStore = mockk<BackupMetadataStore>()
    private val remoteWriteMutex = RemoteWriteMutex()
    private val logger = mockk<DiagnosticsLogger>(relaxed = true)

    private val useCase = DeleteUserAccountUseCase(
        authRepository = authRepository,
        backupEraser = backupEraser,
        backupMetadataStore = backupMetadataStore,
        remoteWriteMutex = remoteWriteMutex,
        logger = logger,
    )

    private fun authenticated(userId: String) =
        SessionStatus.Authenticated(AuthUser(userId = userId, email = "$userId@example.com"))

    @Test
    fun `happy path erases the cloud backups, then clears backup metadata, then calls deleteAccount`() = runTest {
        val userId = "uid-1"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        every { backupMetadataStore.clear(userId) } just Runs
        coEvery { backupEraser.eraseOwnedBackups(userId) } just Runs
        coEvery { authRepository.deleteAccount() } just Runs

        useCase()

        coVerifyOrder {
            backupEraser.eraseOwnedBackups(userId)
            backupMetadataStore.clear(userId)
            authRepository.deleteAccount()
        }
    }

    @Test
    fun `a refused backup sweep leaves the account alive with its backups still switched on`() = runTest {
        val userId = "uid-erase"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { backupEraser.eraseOwnedBackups(userId) } throws DomainException.BackupsNotErased(failedCount = 2)

        val thrown = assertFailsWith<DomainException.BackupsNotErased> { useCase() }

        assertTrue(thrown.failedCount == 2, "the survivor count must reach the caller: ${thrown.failedCount}")
        coVerify(exactly = 0) { authRepository.deleteAccount() }
        verify(exactly = 0) { backupMetadataStore.clear(any()) }
    }

    @Test
    fun `a refused backup sweep logs the backup erase step and rethrows the original exception unchanged`() = runTest {
        every { authRepository.sessionStatus } returns flowOf(authenticated("uid-erase-log"))
        val original = DomainException.BackupsNotErased(failedCount = 1)
        coEvery { backupEraser.eraseOwnedBackups(any()) } throws original

        val thrown = assertFailsWith<DomainException.BackupsNotErased> { useCase() }

        assertTrue(thrown === original, "The original exception instance must reach the caller unchanged")
        verify(exactly = 1) { logger.warn(match { it.contains("backup erase") }, original) }
    }

    @Test
    fun `cancelling the caller during the backup sweep is not logged as a failure and deletes nothing`() = runTest {
        every { authRepository.sessionStatus } returns flowOf(authenticated("uid-erase-cancel"))
        val gate = CompletableDeferred<Unit>()
        coEvery { backupEraser.eraseOwnedBackups(any()) } coAnswers { gate.await() }

        val job = launch { useCase() }
        testScheduler.advanceUntilIdle()
        job.cancelAndJoin()

        verify(exactly = 0) { logger.warn(any(), any()) }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }

    @Test
    fun `NotAuthenticated session throws Unauthorized and makes zero calls to the sweep or deleteAccount`() = runTest {
        every { authRepository.sessionStatus } returns
            flowOf(SessionStatus.Initializing, SessionStatus.NotAuthenticated)

        assertFailsWith<DomainException.Unauthorized> { useCase() }

        coVerify(exactly = 0) { backupEraser.eraseOwnedBackups(any()) }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
        verify(exactly = 0) { backupMetadataStore.clear(any()) }
    }

    @Test
    fun `a failed remote delete leaves the account alive with its metadata already cleared`() = runTest {
        val userId = "uid-2"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        every { backupMetadataStore.clear(userId) } just Runs
        coEvery { backupEraser.eraseOwnedBackups(userId) } just Runs
        coEvery { authRepository.deleteAccount() } throws
            DomainException.NetworkUnavailable(RuntimeException("timeout"))

        assertFailsWith<DomainException.NetworkUnavailable> { useCase() }

        verify(exactly = 1) { backupMetadataStore.clear(userId) }
    }

    @Test
    fun `deleteAccount throwing logs the remote delete step and rethrows the original exception unchanged`() = runTest {
        val userId = "uid-5"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        every { backupMetadataStore.clear(userId) } just Runs
        coEvery { backupEraser.eraseOwnedBackups(userId) } just Runs
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
        every { backupMetadataStore.clear(userId) } just Runs
        coEvery { backupEraser.eraseOwnedBackups(userId) } just Runs
        val gate = CompletableDeferred<Unit>()
        coEvery { authRepository.deleteAccount() } coAnswers { gate.await() }

        val job = launch { useCase() }
        testScheduler.advanceUntilIdle()
        job.cancelAndJoin()

        verify(exactly = 0) { logger.warn(any(), any()) }
    }

    @Test
    fun `Initializing followed by Authenticated resolves correctly and proceeds`() = runTest {
        val userId = "uid-3"
        every { authRepository.sessionStatus } returns
            flowOf(SessionStatus.Initializing, authenticated(userId))
        coEvery { backupEraser.eraseOwnedBackups(userId) } just Runs
        coEvery { authRepository.deleteAccount() } just Runs
        every { backupMetadataStore.clear(userId) } just Runs

        useCase()

        coVerify(exactly = 1) { backupEraser.eraseOwnedBackups(userId) }
        coVerify(exactly = 1) { authRepository.deleteAccount() }
        verify(exactly = 1) { backupMetadataStore.clear(userId) }
    }

    @Test
    fun `deletion waits for an in-flight holder of the shared RemoteWriteMutex`() = runTest {
        val userId = "uid-4"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { backupEraser.eraseOwnedBackups(userId) } just Runs
        coEvery { authRepository.deleteAccount() } just Runs
        every { backupMetadataStore.clear(userId) } just Runs

        val syncGate = CompletableDeferred<Unit>()
        val syncJob = launch { remoteWriteMutex.withLock { syncGate.await() } }
        val deleteJob = launch { useCase() }
        // runCurrent, never advanceUntilIdle: virtual time would jump past RemoteWriteMutex's acquisition
        // timeout and turn this waiting deletion into a Busy failure.
        testScheduler.runCurrent()

        coVerify(exactly = 0) { backupEraser.eraseOwnedBackups(userId) }
        coVerify(exactly = 0) { authRepository.deleteAccount() }

        syncGate.complete(Unit)
        testScheduler.advanceUntilIdle()
        syncJob.join()
        deleteJob.join()

        coVerify(exactly = 1) { backupEraser.eraseOwnedBackups(userId) }
        coVerify(exactly = 1) { authRepository.deleteAccount() }
    }

    @Test
    fun `a stuck holder of the shared RemoteWriteMutex fails the deletion instead of blocking it forever`() = runTest {
        val userId = "uid-8"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        coEvery { backupEraser.eraseOwnedBackups(userId) } just Runs
        coEvery { authRepository.deleteAccount() } just Runs

        val stuck = CompletableDeferred<Unit>()
        val holder = launch { remoteWriteMutex.withLock { stuck.await() } }
        testScheduler.advanceUntilIdle()

        assertFailsWith<DomainException.Busy> { useCase() }

        coVerify(exactly = 0) { backupEraser.eraseOwnedBackups(userId) }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
        stuck.complete(Unit)
        holder.join()
    }

    @Test
    fun `a remote delete slower than the lock acquisition timeout still completes`() = runTest {
        val userId = "uid-9"
        every { authRepository.sessionStatus } returns flowOf(authenticated(userId))
        every { backupMetadataStore.clear(userId) } just Runs
        coEvery { backupEraser.eraseOwnedBackups(userId) } just Runs
        coEvery { authRepository.deleteAccount() } coAnswers { delay(10.minutes) }

        useCase()

        coVerify(exactly = 1) { authRepository.deleteAccount() }
    }

    @Test
    fun `a session stuck in Initializing fails with NetworkUnavailable and releases the sync mutex`() = runTest {
        every { authRepository.sessionStatus } returns MutableStateFlow(SessionStatus.Initializing)

        assertFailsWith<DomainException.NetworkUnavailable> { useCase() }

        coVerify(exactly = 0) { backupEraser.eraseOwnedBackups(any()) }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
        verify(exactly = 0) { backupMetadataStore.clear(any()) }

        assertTrue(remoteWriteMutex.withLock { true }, "The shared RemoteWriteMutex must be free again")
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
