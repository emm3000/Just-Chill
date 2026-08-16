package com.emm.justchill.core.backup

import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupFailureState
import com.emm.domain.shared.backup.BackupMetadataStore
import com.emm.domain.shared.backup.BackupPruneReport
import com.emm.domain.shared.backup.BackupPruner
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupUploader
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.error.ValidationCode
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * `BackupOrchestrator.health` — the persisted state surface ADR 009 Phase 3 (unit 3a-ii) adds beside
 * the one-shot `BackupEvent`.
 *
 * A class of its own rather than more tests in [BackupOrchestratorTest], which is already at
 * detekt's `LargeClass` budget: this is a different question about the same object — not "did a
 * snapshot happen" but "what does this device now say about whether backup works". The harness below
 * is deliberately a copy rather than a shared base class, the same split `ProfileViewModelTest` and
 * `ProfileViewModelImportTest` already make; a base class two suites inherit becomes the place every
 * later fixture is added to, and a stub added for one suite silently changes the other.
 *
 * The store is faked with a real in-memory map, not a fixed `returns`. A stub answering
 * `BackupFailureState(1, …)` forever would make every streak assertion agree with itself — the count
 * would be whatever the stub says rather than whatever the orchestrator asked for, and "two failures
 * then a success leaves zero" would pass against code that never increments anything.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupOrchestratorHealthTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val backupRepository = mockk<BackupRepository>(relaxed = true)
    private val uploader = mockk<BackupUploader>(relaxed = true)
    private val pruner = mockk<BackupPruner>(relaxed = true)
    private val metadata = mockk<BackupMetadataStore>(relaxed = true)
    private val observeSession = mockk<ObserveSessionUseCase>(relaxed = true)
    private val logger = mockk<DiagnosticsLogger>(relaxed = true)

    private val sessionFlow = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
    private val backgroundFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    private val resumeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    private val failures = mutableMapOf<String, BackupFailureState>()

    @Before
    fun setUp() {
        every { observeSession.invoke() } returns sessionFlow
        coEvery { backupRepository.exportToJson(any(), any()) } returns PAYLOAD
        coEvery { pruner.prune() } returns BackupPruneReport(kept = 1, deleted = 0, failedDeletes = emptyList())
        every { metadata.lastSuccessfulBackupAt(any()) } returns null
        every { metadata.failureState(any()) } answers { failures[firstArg()] ?: BackupFailureState.None }
        every { metadata.recordFailure(any(), any()) } answers {
            val userId = firstArg<String>()
            val next = BackupFailureState(
                consecutiveFailures = (failures[userId]?.consecutiveFailures ?: 0) + 1,
                lastReason = secondArg(),
            )
            failures[userId] = next
            next
        }
        every { metadata.clearFailures(any()) } answers { failures -= firstArg<String>() }
    }

    private fun TestScope.buildOrchestrator(): BackupOrchestrator {
        val sharedDispatcher = StandardTestDispatcher(testScheduler)
        return BackupOrchestrator(
            backupRepository = backupRepository,
            uploader = uploader,
            pruner = pruner,
            metadata = metadata,
            observeSession = observeSession,
            appVersion = APP_VERSION,
            clock = fixedClock(NOW),
            timeZone = TimeZone.UTC,
            externalScope = CoroutineScope(sharedDispatcher + SupervisorJob()),
            backgroundEvents = backgroundFlow,
            resumeEvents = resumeFlow,
            logger = logger,
        )
    }

    /** Signs in and drains, so the merged trigger is subscribed before anything is emitted into it. */
    private fun TestScope.authenticate() {
        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = USER_ID, email = "a@b.com"))
        advanceUntilIdle()
    }

    /**
     * The whole reason `health` is a `StateFlow` and not another `BackupEvent`: it has to be readable
     * the next time somebody opens the screen, including on a device that has since restarted. This
     * is the sign-in seam — the only moment the orchestrator learns what this account's history is.
     */
    @Test
    fun `signing in publishes the streak this account already had on disk`() = runTest(testDispatcher) {
        val lastSuccess = Instant.parse("2026-08-10T12:00:00Z").toEpochMilliseconds()
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns lastSuccess
        failures[USER_ID] = BackupFailureState(2, BackupFailureReason.Network)

        val orchestrator = buildOrchestrator()
        orchestrator.start()

        assertEquals(BackupHealth.None, orchestrator.health.value, "Nothing is known before a session resolves")

        authenticate()

        assertEquals(BackupHealth(lastSuccess, 2, BackupFailureReason.Network), orchestrator.health.value)
    }

    @Test
    fun `signing out publishes None rather than leaving the previous account's streak on screen`() =
        runTest(testDispatcher) {
            failures[USER_ID] = BackupFailureState(3, BackupFailureReason.Unverified)

            val orchestrator = buildOrchestrator()
            orchestrator.start()
            authenticate()
            assertEquals(3, orchestrator.health.value.consecutiveFailures)

            sessionFlow.value = SessionStatus.NotAuthenticated
            advanceUntilIdle()

            assertEquals(BackupHealth.None, orchestrator.health.value)
        }

    /**
     * Every failure increments, and the reason travels with the count. Two different failures in a
     * row must leave the SECOND one's reason: a streak labelled with the first outage sends whoever
     * reads it after the newest failure to the wrong place entirely.
     */
    @Test
    fun `consecutive failures increment and health names the newest reason`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("no net"))

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()
        assertEquals(BackupHealth(null, 1, BackupFailureReason.Network), orchestrator.health.value)

        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.ValidationError("read back does not match", ValidationCode.BackupUploadUnverified)
        resumeFlow.emit(Unit)
        advanceUntilIdle()

        assertEquals(BackupHealth(null, 2, BackupFailureReason.Unverified), orchestrator.health.value)
    }

    /**
     * And a verified snapshot is the only thing that says the outage is over, so it clears the streak
     * beside the watermark it records. A streak that survived its own fix would keep a warning on a
     * device that is backing up perfectly.
     */
    @Test
    fun `a verified success clears the streak and publishes the new watermark`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("no net"))
        failures[USER_ID] = BackupFailureState(4, BackupFailureReason.Serialization)
        // The watermark read is wired to the watermark WRITE, not to a stub swapped mid-test: a
        // fixed `returns NOW` would also make the ledger look clean and the second cycle would
        // never run, quietly turning this into a test of the dirty check.
        val recorded = mutableListOf<Long>()
        every { metadata.lastSuccessfulBackupAt(USER_ID) } answers { recorded.lastOrNull() }
        every { metadata.setLastSuccessfulBackupAt(USER_ID, any()) } answers { recorded += secondArg<Long>() }

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()
        assertEquals(5, orchestrator.health.value.consecutiveFailures)

        coEvery { uploader.upload(any(), any(), any()) } returns Unit
        resumeFlow.emit(Unit)
        advanceUntilIdle()

        verify(exactly = 1) { metadata.clearFailures(USER_ID) }
        assertEquals(BackupHealth(NOW.toEpochMilliseconds(), 0, null), orchestrator.health.value)
    }

    /**
     * `SnapshotOutcome.OwnerChanged` is a **refusal, not a failure**: the export ran, the upload was
     * accepted and the read-back verified it — nothing in the pipeline is broken, and the account it
     * would be counted against has left. Counting it would put a failure streak in front of whoever
     * signed in next for an event that was this class refusing to record a good snapshot.
     */
    @Test
    fun `an owner-changed refusal after a verified upload increments no streak`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        coEvery { uploader.upload(USER_ID, any(), any()) } coAnswers {
            sessionFlow.value = SessionStatus.NotAuthenticated
            delay(1)
        }

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        verify(exactly = 0) { metadata.recordFailure(any(), any()) }
        verify(exactly = 0) { metadata.clearFailures(any()) }
        assertEquals(BackupHealth.None, orchestrator.health.value)
    }

    /**
     * The account-switch guard, on the failure side. A cycle that started as A and fails after B has
     * signed in is A's failure: it is booked against the account it CAPTURED, never against whoever
     * is signed in by the time the throwable lands — that is the same mistake the watermark's
     * post-upload re-check exists to prevent, pointing the other way.
     *
     * And it is not published, because `health` describes the account signed in now. B did not fail
     * anything; showing B a streak of 1 is the same lie as recording A's watermark under B.
     */
    @Test
    fun `a failure after an account switch is booked against the account that ran the cycle, not the new one`() =
        runTest(testDispatcher) {
            coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
            coEvery { backupRepository.exportToJson(any(), any()) } coAnswers {
                sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = OTHER_USER_ID, email = "b@b.com"))
                // Parked so the session collector — a different coroutine on the same scheduler —
                // observes the switch before this cycle fails, which is what makes the captured and
                // the live account genuinely different by then.
                delay(1)
                PAYLOAD
            }
            coEvery { uploader.upload(USER_ID, any(), any()) } throws DomainException.Unauthorized(OWNER_CHANGED)

            val orchestrator = buildOrchestrator()
            orchestrator.start()
            authenticate()

            backgroundFlow.emit(Unit)
            advanceUntilIdle()

            verify(exactly = 1) { metadata.recordFailure(USER_ID, BackupFailureReason.Unauthorized) }
            verify(exactly = 0) { metadata.recordFailure(OTHER_USER_ID, any()) }
            assertEquals(
                BackupHealth.None,
                orchestrator.health.value,
                "The account that just signed in must not inherit the previous one's failure",
            )
        }

    /**
     * A manual tap with no session never becomes a cycle and has no account to count against — the
     * failure it publishes on `events` is an answer to the tap, not a statement about any account's
     * backup health. Booking it against the last account signed in would be inventing data.
     */
    @Test
    fun `a request refused for having no session increments nobody's streak`() = runTest(testDispatcher) {
        val orchestrator = buildOrchestrator()
        orchestrator.start()

        sessionFlow.value = SessionStatus.NotAuthenticated
        advanceUntilIdle()

        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        verify(exactly = 0) { metadata.recordFailure(any(), any()) }
    }

    /**
     * ADR 009 Phase 3's other half: "every failure logs a distinct reason". The log line is what
     * whoever is holding an outage reads first, and before this it named only the exception's class —
     * which for the two most common backup failures is `Unknown` twice over.
     */
    @Test
    fun `each failure logs its own named reason and the streak it produced`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.SerializationError(RuntimeException("bad manifest"))

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        verify(exactly = 1) {
            logger.warn(
                match { it.startsWith(CYCLE_FAILED) && it.contains("reason=Serialization") && it.contains(STREAK_1) },
                any(),
            )
        }

        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("no net"))
        resumeFlow.emit(Unit)
        advanceUntilIdle()

        verify(exactly = 1) {
            logger.warn(
                match { it.startsWith(CYCLE_FAILED) && it.contains("reason=Network") && it.contains(STREAK_2) },
                any(),
            )
        }
    }

    /**
     * The check-then-act window inside `publishHealth`, with the interleaving FORCED rather than
     * hoped for.
     *
     * In production `externalScope` is `Dispatchers.Default` (`CoreModule`'s `appScope`), so the
     * session collector and the request consumer are different coroutines on different threads and
     * this ordering is reachable: the consumer passes the "is this still my account" guard, the
     * session ends and publishes [BackupHealth.None], and only then does the consumer's assignment
     * land — restoring the departed account's streak onto a signed-out device. It sticks, because a
     * signed-out session `StateFlow` emits nothing further.
     *
     * **This test runs on real threads on purpose.** An earlier version used
     * `UnconfinedTestDispatcher` on the theory that an emission would resume the collector inline;
     * it does not — `flatMapLatest` buffers through an internal channel — so the collector ran
     * *after* the cycle finished, which is the benign ordering, and the test passed against the
     * unfixed code. A test that cannot fail is worse than no test, so the ordering is now imposed:
     * the stub on the watermark read — the one call `publishHealth` makes between its guard and its
     * assignment — signs out and then BLOCKS until the collector has actually published `None`. Only
     * then does it return, letting the consumer complete its write into a session that is already
     * gone.
     *
     * Every wait is bounded by [AWAIT_TIMEOUT_MILLIS] and fails loudly, so a regression that stops
     * reaching the window shows up as a failure rather than a hang.
     */
    // InjectDispatcher: the rule wants a dispatcher injected, and this test injects one — a REAL
    // multithreaded one, deliberately. It is the subject: the interleaving being proven cannot
    // occur on a single-threaded test dispatcher, and production uses exactly this dispatcher
    // (CoreModule's appScope is Dispatchers.Default + SupervisorJob).
    @Suppress("InjectDispatcher")
    @Test
    fun `a sign-out inside publishHealth leaves no stale account health`() = runBlocking {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("no net"))

        lateinit var orchestrator: BackupOrchestrator
        var signedOutMidPublish = false
        every { metadata.lastSuccessfulBackupAt(USER_ID) } answers {
            // Only from inside publishHealth's window. The seeding publish at sign-in and the
            // dirty check both read this too, and they run before any failure is recorded — so
            // the recorded streak is what identifies the one call that matters.
            if (failures.containsKey(USER_ID) && !signedOutMidPublish) {
                signedOutMidPublish = true
                sessionFlow.value = SessionStatus.NotAuthenticated
                // Publishing None is the collector's LAST act; observing it means currentUserId
                // is already null, which is precisely the state the guard above read as A.
                awaitUntil("the sign-out to reach the orchestrator") {
                    orchestrator.health.value == BackupHealth.None
                }
            }
            LAST_SUCCESS
        }

        orchestrator = BackupOrchestrator(
            backupRepository = backupRepository,
            uploader = uploader,
            pruner = pruner,
            metadata = metadata,
            observeSession = observeSession,
            appVersion = APP_VERSION,
            clock = fixedClock(NOW),
            timeZone = TimeZone.UTC,
            externalScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
            backgroundEvents = backgroundFlow,
            resumeEvents = resumeFlow,
            logger = logger,
        )
        orchestrator.start()
        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = USER_ID, email = "a@b.com"))
        // The trigger is a shared flow with no replay: emitting before it is subscribed drops the
        // event and the cycle never runs.
        withTimeout(AWAIT_TIMEOUT_MILLIS) { backgroundFlow.subscriptionCount.first { it > 0 } }

        backgroundFlow.emit(Unit)
        awaitUntil("the cycle to start") { orchestrator.isBackingUp.value }
        awaitUntil("the cycle to finish") { !orchestrator.isBackingUp.value }

        assertTrue(signedOutMidPublish, "The test never reached publishHealth's window")
        assertEquals(
            BackupHealth.None,
            orchestrator.health.value,
            "A signed-out device must not be showing the departed account's backup health",
        )
    }

    /** Bounded spin — a concurrency test may wait, but it may never hang the suite. */
    private fun awaitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + AWAIT_TIMEOUT_MILLIS * NANOS_PER_MILLI
        while (!condition()) {
            check(System.nanoTime() < deadline) { "Timed out waiting for $what" }
            Thread.sleep(1)
        }
    }

    private fun fixedClock(instant: Instant): Clock = object : Clock {
        override fun now(): Instant = instant
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-08-14T12:00:00Z")
        val CHANGED_AT: Long = Instant.parse("2026-08-14T10:00:00Z").toEpochMilliseconds()
        const val USER_ID = "uid-1"

        /** The account the session switches to mid-cycle. */
        const val OTHER_USER_ID = "uid-2"
        const val APP_VERSION = "2.4.0"

        /** A watermark the departed account owns, so leaking it on screen is unmistakable. */
        val LAST_SUCCESS: Long = Instant.parse("2026-08-10T12:00:00Z").toEpochMilliseconds()

        /**
         * Spelled out here rather than read from production: a test that builds its expectation out
         * of the code under test agrees with any regression that code introduces.
         */
        const val CYCLE_FAILED = "backup cycle failed"

        /**
         * The whole `consecutive failures=N` fragment, not a bare digit. `contains("1")` was
         * satisfied by a streak of 12, 21 or the timestamp in any other number the line happens to
         * carry — an assertion that cannot fail is worse than none, because it reads like coverage.
         */
        const val STREAK_1 = "consecutive failures=1"
        const val STREAK_2 = "consecutive failures=2"

        /** Generous enough for a loaded CI box, short enough that a hang is reported as one. */
        const val AWAIT_TIMEOUT_MILLIS = 5_000L
        const val NANOS_PER_MILLI = 1_000_000L

        /** Stands in for the uploader's own owner-mismatch refusal; its text is pinned in `:data`. */
        const val OWNER_CHANGED = "Snapshot backup failed: the signed-in account changed"
        const val PAYLOAD = """{"schemaVersion":3}"""
    }
}
