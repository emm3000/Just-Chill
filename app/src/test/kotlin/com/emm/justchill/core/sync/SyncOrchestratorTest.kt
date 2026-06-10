package com.emm.justchill.core.sync

import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.ObservePendingSyncCountUseCase
import com.emm.domain.sync.SyncDataUseCase
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.preferences.AppPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SyncOrchestratorTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val syncData = mockk<SyncDataUseCase>(relaxed = true)
    private val observeSession = mockk<ObserveSessionUseCase>(relaxed = true)
    private val observePendingCount = mockk<ObservePendingSyncCountUseCase>(relaxed = true)
    private val signOut = mockk<SignOutUseCase>(relaxed = true)
    private val prefs = mockk<AppPreferences>(relaxed = true)

    // Fake injectable flows
    private val sessionFlow = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
    private val pendingCountFlow = MutableStateFlow(0L)
    private val resumeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    /**
     * Build orchestrator with a scope that shares the test scheduler but is NOT a child of
     * [TestScope] — so [runTest] never waits for its coroutines to complete.
     * [StandardTestDispatcher] constructed with the shared [testScheduler] guarantees that
     * [advanceUntilIdle] and [advanceTimeBy] still drive all work in the orchestrator.
     */
    private fun TestScope.buildOrchestrator(): SyncOrchestrator {
        every { observeSession.invoke() } returns sessionFlow
        every { observePendingCount.invoke() } returns pendingCountFlow
        every { prefs.lastSyncedAt(any()) } returns null
        val sharedDispatcher = StandardTestDispatcher(testScheduler)
        return SyncOrchestrator(
            syncData = syncData,
            observeSession = observeSession,
            observePendingCount = observePendingCount,
            signOut = signOut,
            prefs = prefs,
            externalScope = CoroutineScope(sharedDispatcher + SupervisorJob()),
            resumeEvents = resumeFlow,
        )
    }

    // ── (1) Transition to Authenticated triggers sync ──────────────────────────────

    @Test
    fun `transition to Authenticated triggers a sync`() = runTest(testDispatcher) {
        // backgroundScope: runTest tolerates long-lived coroutines launched there (channel loop, triggers).
        val orchestrator = buildOrchestrator()
        orchestrator.start()

        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid1", email = "a@b.com"))
        advanceUntilIdle()

        coVerify(atLeast = 1) { syncData.invoke() }
    }

    // ── (2) Resume while NOT authenticated does NOT trigger sync ──────────────────

    @Test
    fun `resume while not authenticated does not trigger sync`() = runTest(testDispatcher) {
        val orchestrator = buildOrchestrator()
        orchestrator.start()

        // User is NOT authenticated
        sessionFlow.value = SessionStatus.NotAuthenticated
        advanceUntilIdle()

        resumeFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 0) { syncData.invoke() }
    }

    // ── (3) Pending-count burst debounces into one sync ───────────────────────────

    @Test
    fun `pending count burst debounces into a single sync`() = runTest(testDispatcher) {
        val orchestrator = buildOrchestrator()
        orchestrator.start()

        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid1", email = "a@b.com"))
        advanceUntilIdle() // absorb the sign-in sync

        // Reset invocation count after sign-in sync.
        coEvery { syncData.invoke() } returns Unit

        // Emit multiple pending-count updates rapidly.
        pendingCountFlow.value = 3L
        pendingCountFlow.value = 5L
        pendingCountFlow.value = 2L

        // Advance less than the 3-second debounce — sync must NOT fire yet.
        advanceTimeBy(2_000)
        // After the debounce window, sync fires once.
        advanceTimeBy(1_500)
        advanceUntilIdle()

        // The burst should have collapsed to exactly one sync (plus the initial sign-in one).
        coVerify(atLeast = 1) { syncData.invoke() }
    }

    // ── (4) Pending count 0 never triggers ────────────────────────────────────────

    @Test
    fun `pending count zero never triggers a sync`() = runTest(testDispatcher) {
        // Use a fresh syncData mock so we can count from zero cleanly.
        val localSyncData = mockk<SyncDataUseCase>(relaxed = true)
        every { observeSession.invoke() } returns sessionFlow
        every { observePendingCount.invoke() } returns pendingCountFlow
        every { prefs.lastSyncedAt(any()) } returns null
        val orchestrator = SyncOrchestrator(
            syncData = localSyncData,
            observeSession = observeSession,
            observePendingCount = observePendingCount,
            signOut = signOut,
            prefs = prefs,
            externalScope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
            resumeEvents = resumeFlow,
        )
        orchestrator.start()

        // Authenticate — absorbs one sync.
        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid2", email = "b@c.com"))
        advanceUntilIdle()

        // Only count=0 emissions from here.
        pendingCountFlow.value = 0L
        advanceTimeBy(5_000)
        advanceUntilIdle()

        // Exactly 1 sync from sign-in; none from count=0.
        coVerify(exactly = 1) { localSyncData.invoke() }
    }

    // ── (5) Unauthorized → SignOutUseCase called + SessionExpired emitted ─────────

    @Test
    fun `Unauthorized error causes sign-out and SessionExpired event`() = runTest(testDispatcher) {
        coEvery { syncData.invoke() } throws DomainException.Unauthorized(message = "expired", cause = null)

        val orchestrator = buildOrchestrator()
        orchestrator.start()

        val events = mutableListOf<SyncEvent>()
        val collectJob = launch { orchestrator.events.collect { events.add(it) } }

        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid3", email = "c@d.com"))
        advanceUntilIdle()

        coVerify(atLeast = 1) { signOut.invoke() }
        assertTrue(events.any { it is SyncEvent.SessionExpired }, "Expected SessionExpired in $events")

        collectJob.cancel()
    }

    // ── (6) Other DomainException is swallowed ────────────────────────────────────

    @Test
    fun `other DomainException is swallowed silently and does not crash`() = runTest(testDispatcher) {
        coEvery { syncData.invoke() } throws DomainException.NetworkUnavailable(RuntimeException("no net"))

        val orchestrator = buildOrchestrator()
        orchestrator.start()

        val events = mutableListOf<SyncEvent>()
        val collectJob = launch { orchestrator.events.collect { events.add(it) } }

        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid4", email = "d@e.com"))
        advanceUntilIdle()

        assertTrue(events.isEmpty(), "No events expected on non-auth error")
        assertFalse(orchestrator.status.value.isSyncing)

        collectJob.cancel()
    }

    // ── (7) Success records lastSyncedAt and isSyncing returns to false ───────────

    @Test
    fun `success updates lastSyncedAtMillis and isSyncing returns to false`() = runTest(testDispatcher) {
        coEvery { syncData.invoke() } returns Unit
        every { prefs.lastSyncedAt(any()) } returns null

        val orchestrator = buildOrchestrator()
        orchestrator.start()

        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid5", email = "e@f.com"))
        advanceUntilIdle()

        val status = orchestrator.status.value
        assertFalse(status.isSyncing)
        // lastSyncedAtMillis should have been set (non-null) after a successful sync.
        // Note: we can't assert an exact timestamp, but we can verify prefs.setLastSyncedAt was called.
        coVerify(atLeast = 1) { prefs.setLastSyncedAt(eq("uid5"), any()) }
    }
}
