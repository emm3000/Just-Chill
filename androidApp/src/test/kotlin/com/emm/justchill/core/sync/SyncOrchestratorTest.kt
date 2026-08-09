package com.emm.justchill.core.sync

import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.domain.sync.ObservePendingSyncCountUseCase
import com.emm.domain.sync.SyncDataUseCase
import com.emm.domain.sync.SyncLogger
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.preferences.AppPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
    private val logger = mockk<SyncLogger>(relaxed = true)

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
            logger = logger,
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
            logger = logger,
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

    // ── (8) Manual sync failure emits SyncFailed ──────────────────────────────────

    @Test
    fun `manual sync failure emits SyncFailed with the error`() = runTest(testDispatcher) {
        val networkError = DomainException.NetworkUnavailable(RuntimeException("no net"))
        coEvery { syncData.invoke() } throws networkError

        val orchestrator = buildOrchestrator()
        orchestrator.start()

        val events = mutableListOf<SyncEvent>()
        val collectJob = launch { orchestrator.events.collect { events.add(it) } }

        // Authenticate first so the orchestrator has a session; absorb the automatic sync.
        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid8", email = "h@i.com"))
        advanceUntilIdle()

        // Clear events captured during the automatic cycle, then fire manual.
        events.clear()
        orchestrator.requestSync(manual = true)
        advanceUntilIdle()

        val syncFailedEvents = events.filterIsInstance<SyncEvent.SyncFailed>()
        assertTrue(syncFailedEvents.isNotEmpty(), "Expected at least one SyncFailed event")
        assertTrue(
            syncFailedEvents.any { it.error === networkError },
            "Expected SyncFailed to carry the thrown error",
        )

        collectJob.cancel()
    }

    // ── (9) Automatic sync failure does not emit SyncFailed ───────────────────────

    @Test
    fun `automatic sync failure does not emit SyncFailed`() = runTest(testDispatcher) {
        coEvery { syncData.invoke() } throws DomainException.NetworkUnavailable(RuntimeException("no net"))

        val orchestrator = buildOrchestrator()
        orchestrator.start()

        val events = mutableListOf<SyncEvent>()
        val collectJob = launch { orchestrator.events.collect { events.add(it) } }

        // Trigger via automatic path only (sign-in trigger, no manual = true).
        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid9", email = "i@j.com"))
        advanceUntilIdle()

        val syncFailedEvents = events.filterIsInstance<SyncEvent.SyncFailed>()
        assertTrue(syncFailedEvents.isEmpty(), "Automatic failure must not emit SyncFailed; got $events")

        collectJob.cancel()
    }

    // ── (10) Manual sync with Unauthorized emits SessionExpired but not SyncFailed ─

    @Test
    fun `manual sync with Unauthorized emits SessionExpired but not SyncFailed`() = runTest(testDispatcher) {
        coEvery { syncData.invoke() } throws DomainException.Unauthorized(message = "token expired", cause = null)

        val orchestrator = buildOrchestrator()
        orchestrator.start()

        val events = mutableListOf<SyncEvent>()
        val collectJob = launch { orchestrator.events.collect { events.add(it) } }

        // Authenticate, absorb automatic sync (also Unauthorized → SessionExpired emitted).
        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid10", email = "j@k.com"))
        advanceUntilIdle()

        // Clear events from the automatic cycle, then fire manual.
        events.clear()
        orchestrator.requestSync(manual = true)
        advanceUntilIdle()

        assertTrue(
            events.any { it is SyncEvent.SessionExpired },
            "Expected SessionExpired for Unauthorized, got $events",
        )
        assertTrue(
            events.none { it is SyncEvent.SyncFailed },
            "Must NOT emit SyncFailed for Unauthorized, got $events",
        )

        collectJob.cancel()
    }

    // ── (11) A throwing pending-count flow restarts the trigger instead of killing it ──

    /**
     * The write trigger collects a SQLDelight-backed flow. A mid-observe database failure used to
     * propagate out of the collector into an application scope with no handler — process death.
     * Now the trigger absorbs it and re-subscribes, so a later write still syncs.
     */
    @Test
    fun `a throwing pending-count flow does not crash and the trigger restarts`() = runTest(testDispatcher) {
        var pendingCountCollections = 0
        every { observeSession.invoke() } returns sessionFlow
        every { observePendingCount.invoke() } answers {
            pendingCountCollections++
            if (pendingCountCollections == 1) {
                flow { throw DomainException.DatabaseError(RuntimeException("disk full")) }
            } else {
                pendingCountFlow
            }
        }
        every { prefs.lastSyncedAt(any()) } returns null
        coEvery { syncData.invoke() } returns Unit

        val orchestrator = SyncOrchestrator(
            syncData = syncData,
            observeSession = observeSession,
            observePendingCount = observePendingCount,
            signOut = signOut,
            prefs = prefs,
            externalScope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
            resumeEvents = resumeFlow,
            logger = logger,
        )
        orchestrator.start()

        // Sign-in sync fires; the write trigger blows up on its first collection and backs off.
        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid11", email = "k@l.com"))
        advanceUntilIdle()

        verify(atLeast = 1) { logger.warn(any(), any()) }
        assertTrue(
            pendingCountCollections >= 2,
            "Trigger must have re-subscribed after the failure; collections=$pendingCountCollections",
        )

        // The restarted trigger is live: a write still debounces into a sync.
        pendingCountFlow.value = 4L
        advanceTimeBy(3_500)
        advanceUntilIdle()

        coVerify(atLeast = 2) { syncData.invoke() }
    }

    // ── (12) A non-DomainException does not kill the sequential consumer ──────────

    @Test
    fun `a non-DomainException failure does not kill the sync consumer`() = runTest(testDispatcher) {
        coEvery { syncData.invoke() } throws RuntimeException("boom")

        val orchestrator = buildOrchestrator()
        orchestrator.start()

        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid12", email = "l@m.com"))
        advanceUntilIdle()

        assertTrue(orchestrator.status.value.lastSyncFailed, "lastSyncFailed must be set")
        assertFalse(orchestrator.status.value.isSyncing, "isSyncing must be cleared")

        // The consumer loop must still be draining the channel.
        coEvery { syncData.invoke() } returns Unit
        orchestrator.requestSync()
        advanceUntilIdle()

        coVerify(atLeast = 2) { syncData.invoke() }
        assertFalse(orchestrator.status.value.lastSyncFailed, "A later successful cycle must clear the flag")
    }

    // ── (13) Manual cycle + non-DomainException surfaces as SyncFailed(Unknown) ───

    @Test
    fun `manual cycle failing with a non-DomainException emits SyncFailed carrying Unknown`() =
        runTest(testDispatcher) {
            val boom = RuntimeException("boom")
            coEvery { syncData.invoke() } throws boom

            val orchestrator = buildOrchestrator()
            orchestrator.start()

            val events = mutableListOf<SyncEvent>()
            val collectJob = launch { orchestrator.events.collect { events.add(it) } }

            sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = "uid13", email = "m@n.com"))
            advanceUntilIdle()

            events.clear()
            orchestrator.requestSync(manual = true)
            advanceUntilIdle()

            val failures = events.filterIsInstance<SyncEvent.SyncFailed>()
            assertTrue(failures.isNotEmpty(), "Expected SyncFailed for a manual cycle, got $events")
            assertTrue(
                failures.any { it.error is DomainException.Unknown && it.error.cause === boom },
                "SyncFailed must carry DomainException.Unknown wrapping the original cause, got $failures",
            )

            collectJob.cancel()
        }

    // ── (14) Events emitted with no collector attached survive until one arrives ──

    /**
     * The first sync cycle starts from `bootstrapAppGraph` in Application.onCreate — before any
     * composition exists, so before [com.emm.justchill.hh.shared.SyncEventsHandler] subscribes.
     * A [MutableSharedFlow] with no subscribers discards emissions, so a SessionExpired or a manual
     * SyncFailed raised in that window used to vanish. Buffering is what makes the event survive
     * until the UI is there to show it.
     */
    @Test
    fun `events emitted before any collector attaches are delivered to the first collector`() =
        runTest(testDispatcher) {
            val networkError = DomainException.NetworkUnavailable(RuntimeException("no net"))
            coEvery { syncData.invoke() } throws networkError

            val orchestrator = buildOrchestrator()
            orchestrator.start()

            // Nobody is collecting yet — this is the cold-start window.
            orchestrator.requestSync(manual = true)
            advanceUntilIdle()

            val events = mutableListOf<SyncEvent>()
            val collectJob = launch { orchestrator.events.collect { events.add(it) } }
            advanceUntilIdle()

            assertTrue(
                events.any { it is SyncEvent.SyncFailed && it.error === networkError },
                "The buffered SyncFailed must reach the first collector, got $events",
            )

            collectJob.cancel()
        }
}
