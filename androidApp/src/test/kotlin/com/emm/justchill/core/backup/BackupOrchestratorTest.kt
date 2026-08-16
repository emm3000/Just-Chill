package com.emm.justchill.core.backup

import com.emm.data.backup.backupSnapshotName
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
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The two failure modes of ADR 009 2c-iii-b are DI and lifecycle, and `qualityGate` sees neither: a
 * trigger wired to the wrong flow never crashes, it just silently never backs up. Everything below
 * pins a decision this class is the only thing making — the dirty check, the once-a-day cap on
 * SUCCESSES, the ordering of record-then-prune, and the resilience that keeps one bad cycle from
 * ending backups for the rest of the process lifetime.
 *
 * Shape borrowed from `SyncOrchestratorTest`: the orchestrator's scope shares the test scheduler but
 * is not a child of [TestScope], so `runTest` never waits on its long-lived coroutines, and both
 * lifecycle flows are injected fakes rather than the real `ProcessLifecycleOwner`-backed actuals.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupOrchestratorTest {

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

    /**
     * Defaults only. They are installed HERE and not inside [buildOrchestrator] on purpose: a test
     * that overrides one — the throwing session flow, the failing prune — states its stub before it
     * builds the orchestrator, and a builder that re-stubbed would silently undo it and leave the
     * test asserting the happy path under a name promising the opposite.
     *
     * The three health stubs are flat values on purpose: every cycle now touches the failure streak,
     * but nothing in THIS class asserts on it. The streak's own behaviour — increment, reset,
     * per-account isolation — belongs to `BackupOrchestratorHealthTest`, which fakes the store for
     * real instead.
     */
    @Before
    fun setUp() {
        every { observeSession.invoke() } returns sessionFlow
        coEvery { backupRepository.exportToJson(any(), any()) } returns PAYLOAD
        coEvery { pruner.prune() } returns BackupPruneReport(kept = 1, deleted = 0, failedDeletes = emptyList())
        every { metadata.lastSuccessfulBackupAt(any()) } returns null
        every { metadata.failureState(any()) } returns BackupFailureState.None
        every { metadata.recordFailure(any(), any()) } returns BackupFailureState(1, BackupFailureReason.Unknown)
    }

    private fun TestScope.buildOrchestrator(now: Instant = NOW, zone: TimeZone = TimeZone.UTC): BackupOrchestrator {
        val sharedDispatcher = StandardTestDispatcher(testScheduler)
        return BackupOrchestrator(
            backupRepository = backupRepository,
            uploader = uploader,
            pruner = pruner,
            metadata = metadata,
            observeSession = observeSession,
            appVersion = APP_VERSION,
            clock = fixedClock(now),
            timeZone = zone,
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

    // ── (1) The happy path, and the ORDER inside it ──────────────────────────────────

    @Test
    fun `a dirty ledger not yet backed up today uploads, records, then prunes - in that order`() =
        runTest(testDispatcher) {
            coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
            every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null

            val orchestrator = buildOrchestrator()
            orchestrator.start()
            authenticate()

            backgroundFlow.emit(Unit)
            advanceUntilIdle()

            // The order is the contract: a manifest-verified upload, THEN the watermark, THEN
            // retention. Recording before the upload would let a failure burn the day; pruning
            // before recording would let a prune failure lose a backup that already succeeded.
            coVerifyOrder {
                backupRepository.exportToJson(NOW.toEpochMilliseconds(), APP_VERSION)
                uploader.upload(USER_ID, backupSnapshotName(NOW), PAYLOAD)
                metadata.setLastSuccessfulBackupAt(USER_ID, NOW.toEpochMilliseconds())
                pruner.prune()
            }
        }

    // ── (2) Not dirty ────────────────────────────────────────────────────────────────

    @Test
    fun `a ledger unchanged since the last backup uploads nothing`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns CHANGED_AT

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }
    }

    // ── (3) The daily cap, even with local changes waiting ───────────────────────────

    @Test
    fun `a backup already recorded today uploads nothing even when the ledger is dirty`() = runTest(testDispatcher) {
        val backedUpAt = Instant.parse("2026-08-14T09:00:00Z").toEpochMilliseconds()
        coEvery { backupRepository.latestLocalChangeAt() } returns backedUpAt + 1
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns backedUpAt

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }
    }

    // ── (4) An upload failure must NOT record, so the next trigger retries ───────────

    @Test
    fun `a failed upload records no watermark and the next trigger retries`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null
        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("no net"))

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        // This is what makes the cap a cap on successes: nothing was written, so today is not spent.
        verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
        coVerify(exactly = 0) { pruner.prune() }
        verify(atLeast = 1) { logger.warn(any(), any()) }

        // The consumer survived the failure and the ledger is still dirty, so the retry lands.
        coEvery { uploader.upload(any(), any(), any()) } returns Unit
        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 2) { uploader.upload(any(), any(), any()) }
        verify(exactly = 1) { metadata.setLastSuccessfulBackupAt(USER_ID, NOW.toEpochMilliseconds()) }
    }

    // ── (5) A prune failure must not undo or fail the backup ─────────────────────────

    @Test
    fun `a failed prune leaves the watermark recorded and throws nothing`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null
        coEvery { pruner.prune() } throws DomainException.Unknown(RuntimeException("bucket unreachable"))

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        // The snapshot is uploaded and verified; retention is housekeeping over objects already safe.
        verify(exactly = 1) { metadata.setLastSuccessfulBackupAt(USER_ID, NOW.toEpochMilliseconds()) }

        // And these two are what make `prune()`'s own try/catch load-bearing rather than decoration.
        // As control flow it is interchangeable with `runBackup`'s outer catch — the prune is the
        // last statement and the watermark is already written, so deleting it changes no behaviour
        // any other test in this file can see. What it changes is the MESSAGE: the outer catch says
        // "the last-successful watermark is untouched, so the next trigger retries", which here is
        // false twice over — the watermark IS written and the day IS spent — and it would send
        // whoever is holding an outage hunting for a failed upload that succeeded. Constraint 4 is
        // about a failure nobody can see; a failure described as the wrong one is the same defect.
        verify(exactly = 1) { logger.warn(match { it.startsWith(PRUNE_FAILED) }, any()) }
        verify(exactly = 0) { logger.warn(match { it.startsWith(CYCLE_FAILED) }, any()) }
    }

    // ── (6) No session, no backup — on any trigger, manual included ──────────────────

    @Test
    fun `nothing happens on any trigger while not authenticated`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(any()) } returns null

        val orchestrator = buildOrchestrator()
        orchestrator.start()

        sessionFlow.value = SessionStatus.NotAuthenticated
        advanceUntilIdle()

        backgroundFlow.emit(Unit)
        resumeFlow.emit(Unit)
        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        coVerify(exactly = 0) { backupRepository.exportToJson(any(), any()) }
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }
    }

    // ── (6b) Signing out stops the pipeline, not just never signing in ───────────────

    /**
     * Test 6 covers a device that was never authenticated, which `flatMapLatest` handles by never
     * subscribing at all. This one covers the transition — the seam the mid-cycle account switch
     * below also lives on — where the collectors existed and have to be torn down. Nothing structural
     * says the manual path is covered too: [BackupOrchestrator.requestBackup] does not consult the
     * session, so its request reaches the consumer and is refused by the captured-user gate instead.
     */
    @Test
    fun `a trigger after signing out uploads nothing`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(any()) } returns null

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        sessionFlow.value = SessionStatus.NotAuthenticated
        advanceUntilIdle()

        backgroundFlow.emit(Unit)
        resumeFlow.emit(Unit)
        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        coVerify(exactly = 0) { backupRepository.exportToJson(any(), any()) }
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }
        verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
    }

    // ── (6c) The account can change INSIDE a cycle, and both ends have to notice ─────

    /**
     * A cycle captures the account once and then suspends through an export and four round trips
     * under a 120s `transferTimeout`, while the session collector that maintains `currentUserId` runs
     * in another coroutine and the request consumer is never cancelled on sign-out. So A signing out
     * and B signing in mid-cycle is reachable, and it used to mean: A's ledger uploaded under **B's**
     * prefix — RLS accepts it, the key matches the live session — and `setLastSuccessfulBackupAt("A")`
     * marking A backed up for the day on a snapshot that exists nowhere.
     *
     * The refusal itself belongs to the uploader (`DefaultBackupUploaderTest` owns the message and
     * proves nothing is written). What this test owns is the orchestrator's half: it hands over the
     * account it *captured*, so the assertion is reachable at all, and it records nothing for anybody.
     *
     * The switch is staged inside the `exportToJson` stub, with the same `delay(1)` arrangement as
     * "a sign-out landing after the upload records no watermark" below — **before** `uploader.upload`
     * is called, not inside its own answer. Argument expressions are evaluated before a mocked call's
     * body ever runs, so staging the switch inside `upload`'s `coAnswers` would leave `currentUserId`
     * unchanged at the point the call is actually made: the argument would be the captured account
     * either way, and a call site rewritten to read the live field instead would go undetected. Moving
     * the switch earlier, with a real suspension point for the session collector to run, makes the two
     * diverge for real by the time `uploader.upload` is invoked.
     */
    @Test
    fun `an account switch mid-cycle records no watermark for either account`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(any()) } returns null
        coEvery { backupRepository.exportToJson(any(), any()) } coAnswers {
            sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = OTHER_USER_ID, email = "b@b.com"))
            // The session collector is a different coroutine on the same scheduler. Parking here for
            // one virtual millisecond lets it observe the switch before this cycle reaches
            // uploader.upload, so the captured account and the live one are genuinely different by
            // then rather than merely different inside this answer block.
            delay(1)
            PAYLOAD
        }
        coEvery { uploader.upload(USER_ID, any(), any()) } throws DomainException.Unauthorized(OWNER_CHANGED)

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        // The captured account is what travelled — a cycle that read the live field instead would
        // have handed over B, and the uploader would have had nothing to disagree with.
        coVerify(exactly = 1) { uploader.upload(USER_ID, backupSnapshotName(NOW), PAYLOAD) }
        verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
        coVerify(exactly = 0) { pruner.prune() }
    }

    /**
     * And the other half, which the uploader's assertion cannot reach: the session changing **after**
     * the last byte is sent. The upload succeeded, so nothing throws, and only the re-check before the
     * write stops a watermark for an account that is gone. A false watermark is worse than a missing
     * one — it suppresses this account's next backup for a whole day, whereas a missing one costs the
     * retry the next trigger already provides.
     */
    @Test
    fun `a sign-out landing after the upload records no watermark`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(any()) } returns null
        coEvery { uploader.upload(USER_ID, any(), any()) } coAnswers {
            sessionFlow.value = SessionStatus.NotAuthenticated
            // The session collector is a different coroutine on the same scheduler. Parking here for
            // one virtual millisecond lets it observe the sign-out before this cycle reaches its
            // re-check, which is the real ordering rather than a contrivance: the upload returning
            // and the session flow emitting are genuinely independent.
            delay(1)
        }

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { uploader.upload(USER_ID, backupSnapshotName(NOW), PAYLOAD) }
        verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
        // Skipped too: retention would be pruning whatever bucket the session points at now.
        coVerify(exactly = 0) { pruner.prune() }
        verify(exactly = 1) { logger.warn(match { it.startsWith(OWNER_GONE) }, any()) }
    }

    // ── (7) A manual request bypasses BOTH the cap and the dirty check ───────────────

    @Test
    fun `a manual request uploads with a clean ledger already backed up today`() = runTest(testDispatcher) {
        // Not dirty at all (no rows anywhere) AND capped (a success recorded an hour ago today).
        coEvery { backupRepository.latestLocalChangeAt() } returns null
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns
            Instant.parse("2026-08-14T11:00:00Z").toEpochMilliseconds()

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { uploader.upload(USER_ID, backupSnapshotName(NOW), PAYLOAD) }
        verify(exactly = 1) { metadata.setLastSuccessfulBackupAt(USER_ID, NOW.toEpochMilliseconds()) }
    }

    // ── (8) The concurrency guard, from both sides ───────────────────────────────────

    /**
     * Two automatic triggers in one drain produce ONE upload, and the reason is what this test is
     * really for: the second cycle reads the watermark the first one wrote. Run concurrently, both
     * would have read the pre-upload value, both would have found the ledger dirty and uncapped, and
     * two snapshots would have gone into the same bucket in the same second.
     */
    @Test
    fun `two triggers arriving together produce one upload, not two`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        val recorded = mutableListOf<Long>()
        every { metadata.lastSuccessfulBackupAt(USER_ID) } answers { recorded.lastOrNull() }
        every { metadata.setLastSuccessfulBackupAt(USER_ID, any()) } answers { recorded += secondArg<Long>() }

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        resumeFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { uploader.upload(any(), any(), any()) }
    }

    /**
     * And when two cycles genuinely both have work to do — two manual requests, which bypass the cap
     * and the dirty check — they run one after the other rather than at the same time. Concurrent
     * uploads into one bucket are the state ADR 009's naming and read-back verification both assume
     * cannot happen, and only the single consumer draining the request channel prevents it.
     */
    @Test
    fun `two manual requests never have two uploads in flight at once`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null

        var inFlight = 0
        var peakInFlight = 0
        coEvery { uploader.upload(any(), any(), any()) } coAnswers {
            inFlight++
            peakInFlight = maxOf(peakInFlight, inFlight)
            delay(UPLOAD_DURATION_MILLIS)
            inFlight--
        }

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        orchestrator.requestBackup(manual = true)
        // Mid-upload: the second request arrives while the first cycle is still suspended in the
        // uploader, which is the only window in which two could ever overlap.
        advanceTimeBy(UPLOAD_DURATION_MILLIS / 2)
        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        coVerify(exactly = 2) { uploader.upload(any(), any(), any()) }
        assertEquals(1, peakInFlight, "Two uploads were in flight at once")
    }

    // ── (9) A trigger that throws must not end backups for the process ───────────────

    /**
     * The session flow is auth-provider-backed and can throw mid-observe. Collected bare, that
     * throwable reaches an application scope with no handler — process death. The trigger absorbs it
     * and re-subscribes, and without this test that resilience is decoration: everything else here
     * passes whether or not `launchResilientTrigger` exists.
     */
    @Test
    fun `a throwing session flow does not kill the trigger and a later backup still runs`() = runTest(testDispatcher) {
        var sessionCollections = 0
        every { observeSession.invoke() } answers {
            sessionCollections++
            if (sessionCollections == 1) {
                flow { throw DomainException.DatabaseError(RuntimeException("disk full")) }
            } else {
                sessionFlow
            }
        }
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        advanceUntilIdle()

        verify(atLeast = 1) { logger.warn(any(), any()) }

        // Past the back-off, the trigger re-subscribes to a healthy flow.
        advanceTimeBy(RETRY_BACKOFF_MILLIS)
        advanceUntilIdle()
        assertTrue(
            sessionCollections >= 2,
            "Trigger must have re-subscribed after the failure; collections=$sessionCollections",
        )

        authenticate()
        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { uploader.upload(any(), any(), any()) }
    }

    // ── (10) The cap is a CALENDAR question, answered in the injected zone ───────────

    /**
     * The same two instants, four hours apart, straddle midnight in one zone and not in the other.
     * A cap implemented as `now - last < 24h` would answer identically in both and pass every other
     * test in this file.
     */
    @Test
    fun `the daily cap is evaluated in the injected time zone`() = runTest(testDispatcher) {
        val lastSuccessAt = Instant.parse("2026-08-14T02:00:00Z").toEpochMilliseconds()
        val now = Instant.parse("2026-08-14T06:00:00Z")
        coEvery { backupRepository.latestLocalChangeAt() } returns lastSuccessAt + 1
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns lastSuccessAt

        // UTC: 02:00 and 06:00 are the same 14 August, so the cap holds.
        val utc = buildOrchestrator(now = now, zone = TimeZone.UTC)
        utc.start()
        authenticate()
        backgroundFlow.emit(Unit)
        advanceUntilIdle()
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }

        // UTC-5: the same instants are 13 August 21:00 and 14 August 01:00 — a new day, so it runs.
        val lima = buildOrchestrator(now = now, zone = TimeZone.of("America/Lima"))
        lima.start()
        advanceUntilIdle()
        backgroundFlow.emit(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) { uploader.upload(USER_ID, backupSnapshotName(now), PAYLOAD) }
    }

    // ── (11) An empty database is not a backup candidate ─────────────────────────────

    @Test
    fun `an empty ledger touches neither the export, the upload nor the prune`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns null
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        resumeFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 0) { backupRepository.exportToJson(any(), any()) }
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }
        coVerify(exactly = 0) { pruner.prune() }
    }

    // ── (12b) The outcome signal: what a manual tap gets told, and what nothing else does ──

    /**
     * The signal 2c-iv is built on. `requestBackup` returns `Unit`, so without these the Perfil row
     * could only report that a request was *queued* — a green tick for a backup that has not started
     * and may fail thirteen named ways. Everything below pins which endings reach [BackupEvent] and,
     * just as importantly, which do not.
     */
    @Test
    fun `a manual cycle that records a snapshot publishes Succeeded`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null

        val orchestrator = buildOrchestrator()
        val events = mutableListOf<BackupEvent>()
        val job = launch { orchestrator.events.collect { events += it } }
        advanceUntilIdle()

        orchestrator.start()
        authenticate()
        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        assertEquals(listOf<BackupEvent>(BackupEvent.Succeeded), events)

        job.cancel()
    }

    @Test
    fun `a failed manual cycle publishes Failed and never a success`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null
        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("no net"))

        val orchestrator = buildOrchestrator()
        val events = mutableListOf<BackupEvent>()
        val job = launch { orchestrator.events.collect { events += it } }
        advanceUntilIdle()

        orchestrator.start()
        authenticate()
        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        val failure = events.singleOrNull()
        assertTrue(
            failure is BackupEvent.Failed && failure.cause is DomainException.NetworkUnavailable,
            "Expected exactly one Failed(NetworkUnavailable), got: $events",
        )

        job.cancel()
    }

    /**
     * The account-switch refusal, which is the one failure this pipeline produces that another
     * consumer would answer by signing the user out. It arrives here as an ordinary
     * [BackupEvent.Failed] — the orchestrator's job is to say the backup did not happen, and
     * `ProfileViewModel` owns the decision not to translate `Unauthorized` into "sesión expirada".
     */
    @Test
    fun `an account switch mid-cycle answers a manual request with a failure, not a success`() =
        runTest(testDispatcher) {
            coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
            every { metadata.lastSuccessfulBackupAt(any()) } returns null
            coEvery { uploader.upload(USER_ID, any(), any()) } coAnswers {
                sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = OTHER_USER_ID, email = "b@b.com"))
                delay(1)
            }

            val orchestrator = buildOrchestrator()
            val events = mutableListOf<BackupEvent>()
            val job = launch { orchestrator.events.collect { events += it } }
            advanceUntilIdle()

            orchestrator.start()
            authenticate()
            orchestrator.requestBackup(manual = true)
            advanceUntilIdle()

            // The upload itself returned normally, so only the post-upload re-check separates this
            // from a success — which is exactly what a Boolean return could not have expressed.
            verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
            assertTrue(
                events.singleOrNull() is BackupEvent.Failed,
                "A cycle that recorded nothing must not be reported as a success: $events",
            )

            job.cancel()
        }

    /**
     * Automatic cycles say nothing, and that is the whole reason [BackupEvent] is one-shot rather
     * than a status field. Background and resume triggers fire while the user is elsewhere; a
     * snackbar for one of them arrives attached to nothing the user did.
     */
    @Test
    fun `an automatic cycle publishes no event, whether it succeeds or fails`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null

        val orchestrator = buildOrchestrator()
        val events = mutableListOf<BackupEvent>()
        val job = launch { orchestrator.events.collect { events += it } }
        advanceUntilIdle()

        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("no net"))
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null
        resumeFlow.emit(Unit)
        advanceUntilIdle()

        assertEquals(emptyList<BackupEvent>(), events, "Automatic cycles must publish nothing")

        job.cancel()
    }

    /**
     * A manual request made with no session never becomes a cycle — no export, upload, record or
     * prune runs — but it is no longer the ending this class reports nothing for. `ProfileViewModel`
     * refuses a signed-out tap at the button, but that check only sees the session at tap time; this
     * orchestrator is the backstop for the session having already ended by the time the single
     * consumer gets to the request, which is exactly what "was never authenticated" looks like from
     * here — [currentUserId] is null either way. See [BackupOrchestrator.requestBackup]'s KDoc.
     */
    @Test
    fun `a manual request while signed out publishes Failed, not nothing`() = runTest(testDispatcher) {
        val orchestrator = buildOrchestrator()
        val events = mutableListOf<BackupEvent>()
        val job = launch { orchestrator.events.collect { events += it } }
        advanceUntilIdle()

        orchestrator.start()
        sessionFlow.value = SessionStatus.NotAuthenticated
        advanceUntilIdle()

        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        coVerify(exactly = 0) { backupRepository.exportToJson(any(), any()) }
        val failure = events.singleOrNull()
        assertTrue(failure is BackupEvent.Failed, "Expected exactly one Failed, got: $events")

        job.cancel()
    }

    /**
     * The case FIX 2 is actually about: a session that existed at tap time and is gone by the time
     * the single consumer drains the request — the window `ProfileViewModel.backUpNow`'s tap-time
     * check cannot see, because `requestBackup` returns before the session can be re-checked and
     * nothing suspends between the two. This device signs in, the request is queued, and only then
     * does the session end — the same outcome as the fully-signed-out case above, reached the other
     * way round.
     */
    @Test
    fun `a manual request whose session ends before the consumer drains it still gets an answer`() =
        runTest(testDispatcher) {
            val orchestrator = buildOrchestrator()
            val events = mutableListOf<BackupEvent>()
            val job = launch { orchestrator.events.collect { events += it } }
            advanceUntilIdle()

            orchestrator.start()
            authenticate()

            sessionFlow.value = SessionStatus.NotAuthenticated
            advanceUntilIdle()

            orchestrator.requestBackup(manual = true)
            advanceUntilIdle()

            coVerify(exactly = 0) { backupRepository.exportToJson(any(), any()) }
            verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
            val failure = events.singleOrNull()
            assertTrue(failure is BackupEvent.Failed, "Expected exactly one Failed, got: $events")

            job.cancel()
        }

    // ── (13) A request before start() is dropped and logged, not silently queued ─────

    /**
     * [BackupOrchestrator.start] is what [SNAPSHOT_BACKUP_ENABLED] being false keeps uncalled in
     * production, so this is the shape a stray `requestBackup` reaching a not-yet-started (or
     * never-started) orchestrator takes: dropped, logged, and nothing uploaded — never a silent
     * `trySend` onto a channel nothing will ever drain.
     */
    @Test
    fun `a request made before start logs a warning and uploads nothing`() = runTest(testDispatcher) {
        val orchestrator = buildOrchestrator()

        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        verify(exactly = 1) {
            logger.warn(match { it.contains("before the orchestrator started") }, isNull())
        }
        coVerify(exactly = 0) { backupRepository.exportToJson(any(), any()) }
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }
    }

    // ── (12c) isBackingUp is true for the cycle's whole duration, and false after ────

    @Test
    fun `isBackingUp is true while a cycle runs and false once it ends`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null
        coEvery { uploader.upload(any(), any(), any()) } coAnswers { delay(UPLOAD_DURATION_MILLIS) }

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        assertEquals(false, orchestrator.isBackingUp.value, "Idle before anything is requested")

        orchestrator.requestBackup(manual = true)
        advanceTimeBy(UPLOAD_DURATION_MILLIS / 2)
        assertEquals(true, orchestrator.isBackingUp.value, "Must report busy while the upload is in flight")

        advanceUntilIdle()
        assertEquals(false, orchestrator.isBackingUp.value, "Must return to idle once the cycle ends")
    }

    /**
     * Including the cycle that turns out to have nothing to do. The flag wraps the whole body, so it
     * cannot drift out of step with the early returns inside it — and a UI deriving a spinner from it
     * gets a flicker rather than a spinner that never stops.
     */
    @Test
    fun `isBackingUp returns to idle after a request that is discarded for having no session`() =
        runTest(testDispatcher) {
            val orchestrator = buildOrchestrator()
            orchestrator.start()

            sessionFlow.value = SessionStatus.NotAuthenticated
            advanceUntilIdle()

            orchestrator.requestBackup(manual = true)
            advanceUntilIdle()

            assertEquals(false, orchestrator.isBackingUp.value)
        }

    // ── (12) The resume trigger exists, and it is the mid-upload-death retry ─────────

    @Test
    fun `a resume backs up a ledger left dirty by a process that died mid-upload`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns null

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        resumeFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { uploader.upload(USER_ID, backupSnapshotName(NOW), PAYLOAD) }
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

        /**
         * Log-message prefixes, spelled out here rather than read from production — a test that
         * builds its expectation out of the code under test agrees with any regression it introduces.
         * Each one is the only thing separating a swallow from the wrong swallow.
         */
        const val PRUNE_FAILED = "backup prune failed after a verified upload"
        const val CYCLE_FAILED = "backup cycle failed"
        const val OWNER_GONE = "backup upload finished for an account that is no longer signed in"

        /** Stands in for the uploader's own owner-mismatch refusal; its text is pinned in `:data`. */
        const val OWNER_CHANGED = "Snapshot backup failed: the signed-in account changed"
        const val PAYLOAD = """{"schemaVersion":3}"""

        /** Comfortably past `BackupOrchestrator.TRIGGER_RETRY_DELAY`, which is private. */
        const val RETRY_BACKOFF_MILLIS = 6_000L

        /** Long enough that a second request lands squarely inside the first cycle's upload. */
        const val UPLOAD_DURATION_MILLIS = 1_000L
    }
}
