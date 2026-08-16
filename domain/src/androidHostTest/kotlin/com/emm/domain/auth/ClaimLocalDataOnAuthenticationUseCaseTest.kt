package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ClaimLocalDataOnAuthenticationUseCaseTest {

    private val observeSession = mockk<ObserveSessionUseCase>()
    private val claimLocalDataRepository = mockk<ClaimLocalDataRepository>(relaxed = true)
    private val claimLocalData = ClaimLocalDataUseCase(claimLocalDataRepository)
    private val logger = mockk<DiagnosticsLogger>(relaxed = true)

    private val unclaimedCount = MutableStateFlow(0L)

    private val useCase = ClaimLocalDataOnAuthenticationUseCase(
        observeSession,
        claimLocalData,
        claimLocalDataRepository,
        logger,
    )

    private fun authenticated(userId: String) =
        SessionStatus.Authenticated(AuthUser(userId = userId, email = "$userId@example.com"))

    @Test
    fun `authenticated with unclaimed rows triggers a claim`() = runTest {
        every { observeSession.invoke() } returns flowOf(authenticated("uid-1"))
        every { claimLocalDataRepository.observeUnclaimedCount() } returns flowOf(3L)
        coEvery { claimLocalDataRepository.claimAll("uid-1") } just Runs

        useCase()

        coVerify(exactly = 1) { claimLocalDataRepository.claimAll("uid-1") }
    }

    @Test
    fun `authenticated with zero unclaimed rows does not claim`() = runTest {
        every { observeSession.invoke() } returns flowOf(authenticated("uid-1"))
        every { claimLocalDataRepository.observeUnclaimedCount() } returns flowOf(0L)

        useCase()

        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `not authenticated does not claim even if unclaimed rows exist`() = runTest {
        every { observeSession.invoke() } returns flowOf(SessionStatus.Initializing, SessionStatus.NotAuthenticated)
        every { claimLocalDataRepository.observeUnclaimedCount() } returns flowOf(5L)

        useCase()

        coVerify(exactly = 0) { claimLocalDataRepository.claimAll(any()) }
    }

    @Test
    fun `REGRESSION unclaimed count rising while already authenticated claims without new session emission`() =
        runTest {
            val claimedUserIds = mutableListOf<String>()
            val fakeUnclaimedCount = MutableStateFlow(0L)

            val fakeRepo = object : ClaimLocalDataRepository {
                override suspend fun claimAll(userId: String) {
                    claimedUserIds += userId
                }

                override suspend fun unclaimAll(userId: String) = Unit

                override fun observeUnclaimedCount() = fakeUnclaimedCount
            }
            val fakeClaimUseCase = ClaimLocalDataUseCase(fakeRepo)
            val sessionFlow = MutableStateFlow<SessionStatus>(authenticated("uid-1"))
            every { observeSession.invoke() } returns sessionFlow

            val reactiveUseCase = ClaimLocalDataOnAuthenticationUseCase(
                observeSession,
                fakeClaimUseCase,
                fakeRepo,
                logger,
            )

            val job = launch { reactiveUseCase() }
            testScheduler.advanceUntilIdle()

            assert(claimedUserIds.isEmpty()) { "Expected no claims yet, got: $claimedUserIds" }

            fakeUnclaimedCount.value = 1L
            testScheduler.advanceUntilIdle()
            assert(claimedUserIds.size == 1 && claimedUserIds[0] == "uid-1") {
                "Expected 1 claim for uid-1, got: $claimedUserIds"
            }

            fakeUnclaimedCount.value = 2L
            testScheduler.advanceUntilIdle()
            assert(claimedUserIds.size == 2) {
                "Expected 2 claims total, got: $claimedUserIds"
            }

            job.cancel()
        }

    @Test
    fun `a claim failure does not crash the collector — next unclaimed emission retries`() = runTest {
        every { observeSession.invoke() } returns flowOf(authenticated("uid-1"))
        every { claimLocalDataRepository.observeUnclaimedCount() } returns flowOf(1L, 1L)
        coEvery { claimLocalDataRepository.claimAll("uid-1") }
            .throws(DomainException.DatabaseError(RuntimeException("locked")))
            .andThen(Unit)

        useCase()

        coVerify(exactly = 2) { claimLocalDataRepository.claimAll("uid-1") }
    }

    @Test
    fun `a failing unclaimed-count flow is retried and the claim still runs`() = runTest {
        every { observeSession.invoke() } returns flowOf(authenticated("uid-1"))
        var collections = 0
        every { claimLocalDataRepository.observeUnclaimedCount() } answers {
            collections++
            if (collections == 1) {
                flow { throw DomainException.DatabaseError(RuntimeException("disk full")) }
            } else {
                flowOf(1L)
            }
        }
        coEvery { claimLocalDataRepository.claimAll("uid-1") } just Runs

        useCase()

        coVerify(exactly = 1) { claimLocalDataRepository.claimAll("uid-1") }
        verify(atLeast = 1) { logger.warn(any(), any()) }
    }

    @Test
    fun `a swallowed claim failure is logged`() = runTest {
        every { observeSession.invoke() } returns flowOf(authenticated("uid-1"))
        every { claimLocalDataRepository.observeUnclaimedCount() } returns flowOf(1L)
        coEvery { claimLocalDataRepository.claimAll("uid-1") }
            .throws(DomainException.DatabaseError(RuntimeException("locked")))

        useCase()

        verify(atLeast = 1) { logger.warn(any(), any()) }
    }
}
