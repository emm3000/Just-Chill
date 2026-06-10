package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.SyncCursorStore
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class DeleteUserAccountUseCaseTest {

    private val authRepository = mockk<AuthRepository>()
    private val claimLocalDataRepository = mockk<ClaimLocalDataRepository>()
    private val syncCursorStore = mockk<SyncCursorStore>()

    private val useCase = DeleteUserAccountUseCase(
        authRepository = authRepository,
        claimLocalDataRepository = claimLocalDataRepository,
        syncCursorStore = syncCursorStore,
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
}
