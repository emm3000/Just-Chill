package com.emm.domain.auth

import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.SyncLogger
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
    private val logger = mockk<SyncLogger>(relaxed = true)

    // Controllable unclaimed-count flow pushed into per-test.
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

    /**
     * REGRESSION GUARD: while already authenticated (no new session emission), a transition of
     * unclaimed count 0→N triggers a claim. This is the exact bug being fixed.
     *
     * The old code used distinctUntilChanged on the userId, meaning a second insert while
     * authenticated produced no new session emission and claimAll was never called.
     *
     * Uses a fake [ClaimLocalDataRepository] to avoid MockK stubbing complexity for non-suspend
     * Flow-returning methods. Claim invocations are recorded in a list for assertion.
     */
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

            // Launch in background scope (won't block runTest completion) and drain so it subscribes.
            val job = launch { reactiveUseCase() }
            testScheduler.advanceUntilIdle()

            // Initially no unclaimed rows — no claim yet.
            assert(claimedUserIds.isEmpty()) { "Expected no claims yet, got: $claimedUserIds" }

            // New unclaimed row appears (created while user was already authenticated) — must claim.
            fakeUnclaimedCount.value = 1L
            testScheduler.advanceUntilIdle()
            assert(claimedUserIds.size == 1 && claimedUserIds[0] == "uid-1") {
                "Expected 1 claim for uid-1, got: $claimedUserIds"
            }

            // Another row appears — must claim again.
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
        // Two consecutive unclaimed emissions: first throws, second succeeds.
        every { claimLocalDataRepository.observeUnclaimedCount() } returns flowOf(1L, 1L)
        coEvery { claimLocalDataRepository.claimAll("uid-1") }
            .throws(DomainException.DatabaseError(RuntimeException("locked")))
            .andThen(Unit)

        useCase()

        coVerify(exactly = 2) { claimLocalDataRepository.claimAll("uid-1") }
    }

    /**
     * A throw from the unclaimed-count QUERY (not from the claim) used to terminate the observer
     * for the rest of the process lifetime: nothing was ever claimed again, and nothing was
     * logged. `retryWhen` re-subscribes after a delay, so the next successful collection claims.
     *
     * The stateful stub makes the first collection fail and the second succeed; `runTest` skips
     * the 5 s retry delay in virtual time.
     */
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
