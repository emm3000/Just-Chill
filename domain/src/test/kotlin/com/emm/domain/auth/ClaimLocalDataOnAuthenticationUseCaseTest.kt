package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClaimLocalDataOnAuthenticationUseCaseTest {

    private val observeSession = mockk<ObserveSessionUseCase>()
    private val claimLocalDataRepository = mockk<ClaimLocalDataRepository>(relaxed = true)
    private val claimLocalData = ClaimLocalDataUseCase(claimLocalDataRepository)
    private val useCase = ClaimLocalDataOnAuthenticationUseCase(observeSession, claimLocalData)

    private fun authenticated(userId: String) =
        SessionStatus.Authenticated(AuthUser(userId = userId, email = "$userId@example.com"))

    @Test
    fun `claims with the userId when session becomes Authenticated`() = runTest {
        every { observeSession.invoke() } returns flowOf(authenticated("uid-1"))
        coEvery { claimLocalDataRepository.claimAll("uid-1") } just Runs

        useCase()

        coVerify(exactly = 1) { claimLocalDataRepository.claimAll("uid-1") }
    }

    @Test
    fun `does not claim for non-authenticated statuses`() = runTest {
        every { observeSession.invoke() } returns flowOf(SessionStatus.Initializing, SessionStatus.NotAuthenticated)

        useCase()

        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `claims only once for repeated emissions of the same user`() = runTest {
        every { observeSession.invoke() } returns flowOf(authenticated("uid-1"), authenticated("uid-1"))
        coEvery { claimLocalDataRepository.claimAll("uid-1") } just Runs

        useCase()

        coVerify(exactly = 1) { claimLocalDataRepository.claimAll("uid-1") }
    }

    @Test
    fun `a claim failure does not stop the observer from claiming the next user`() = runTest {
        every { observeSession.invoke() } returns flowOf(authenticated("uid-1"), authenticated("uid-2"))
        coEvery { claimLocalDataRepository.claimAll("uid-1") } throws
            DomainException.DatabaseError(RuntimeException("locked"))
        coEvery { claimLocalDataRepository.claimAll("uid-2") } just Runs

        useCase()

        coVerify(exactly = 1) { claimLocalDataRepository.claimAll("uid-1") }
        coVerify(exactly = 1) { claimLocalDataRepository.claimAll("uid-2") }
    }
}
