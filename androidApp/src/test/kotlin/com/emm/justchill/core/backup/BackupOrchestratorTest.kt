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

    // Seeded with USER_ID already disclosed: every test that is not about the disclosure gate is
    // about what happens once it is open. The gate's own tests clear this first.
    private val disclosed = mutableMapOf(USER_ID to DISCLOSED_AT)

    @Before
    fun setUp() {
        every { observeSession.invoke() } returns sessionFlow
        coEvery { backupRepository.exportToJson(any(), any()) } returns PAYLOAD
        coEvery { pruner.prune() } returns BackupPruneReport(kept = 1, deleted = 0, failedDeletes = emptyList())
        every { metadata.lastSuccessfulBackupAt(any()) } returns null
        every { metadata.failureState(any()) } returns BackupFailureState.None
        every { metadata.recordFailure(any(), any()) } returns BackupFailureState(1, BackupFailureReason.Unknown)
        every { metadata.destinationDisclosedAt(any()) } answers { disclosed[firstArg<String>()] }
        every { metadata.setDestinationDisclosed(any(), any()) } answers {
            disclosed[firstArg()] = secondArg()
        }
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

    private fun TestScope.authenticate() {
        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = USER_ID, email = "a@b.com"))
        advanceUntilIdle()
    }

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

            coVerifyOrder {
                backupRepository.exportToJson(NOW.toEpochMilliseconds(), APP_VERSION)
                uploader.upload(USER_ID, backupSnapshotName(NOW), PAYLOAD)
                metadata.setLastSuccessfulBackupAt(USER_ID, NOW.toEpochMilliseconds())
                pruner.prune()
            }
        }

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

        verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
        coVerify(exactly = 0) { pruner.prune() }
        verify(atLeast = 1) { logger.warn(any(), any()) }

        coEvery { uploader.upload(any(), any(), any()) } returns Unit
        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 2) { uploader.upload(any(), any(), any()) }
        verify(exactly = 1) { metadata.setLastSuccessfulBackupAt(USER_ID, NOW.toEpochMilliseconds()) }
    }

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

        verify(exactly = 1) { metadata.setLastSuccessfulBackupAt(USER_ID, NOW.toEpochMilliseconds()) }

        verify(exactly = 1) { logger.warn(match { it.startsWith(PRUNE_FAILED) }, any()) }
        verify(exactly = 0) { logger.warn(match { it.startsWith(CYCLE_FAILED) }, any()) }
    }

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

    @Test
    fun `an account switch mid-cycle records no watermark for either account`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(any()) } returns null
        coEvery { backupRepository.exportToJson(any(), any()) } coAnswers {
            sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = OTHER_USER_ID, email = "b@b.com"))
            delay(1)
            PAYLOAD
        }
        coEvery { uploader.upload(USER_ID, any(), any()) } throws DomainException.Unauthorized(OWNER_CHANGED)

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { uploader.upload(USER_ID, backupSnapshotName(NOW), PAYLOAD) }
        verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
        coVerify(exactly = 0) { pruner.prune() }
    }

    @Test
    fun `a sign-out landing after the upload records no watermark`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        every { metadata.lastSuccessfulBackupAt(any()) } returns null
        coEvery { uploader.upload(USER_ID, any(), any()) } coAnswers {
            sessionFlow.value = SessionStatus.NotAuthenticated
            delay(1)
        }

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { uploader.upload(USER_ID, backupSnapshotName(NOW), PAYLOAD) }
        verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
        coVerify(exactly = 0) { pruner.prune() }
        verify(exactly = 1) { logger.warn(match { it.startsWith(OWNER_GONE) }, any()) }
    }

    @Test
    fun `a manual request uploads with a clean ledger already backed up today`() = runTest(testDispatcher) {
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
        advanceTimeBy(UPLOAD_DURATION_MILLIS / 2)
        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        coVerify(exactly = 2) { uploader.upload(any(), any(), any()) }
        assertEquals(1, peakInFlight, "Two uploads were in flight at once")
    }

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

    @Test
    fun `the daily cap is evaluated in the injected time zone`() = runTest(testDispatcher) {
        val lastSuccessAt = Instant.parse("2026-08-14T02:00:00Z").toEpochMilliseconds()
        val now = Instant.parse("2026-08-14T06:00:00Z")
        coEvery { backupRepository.latestLocalChangeAt() } returns lastSuccessAt + 1
        every { metadata.lastSuccessfulBackupAt(USER_ID) } returns lastSuccessAt

        val utc = buildOrchestrator(now = now, zone = TimeZone.UTC)
        utc.start()
        authenticate()
        backgroundFlow.emit(Unit)
        advanceUntilIdle()
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }

        val lima = buildOrchestrator(now = now, zone = TimeZone.of("America/Lima"))
        lima.start()
        advanceUntilIdle()
        backgroundFlow.emit(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) { uploader.upload(USER_ID, backupSnapshotName(now), PAYLOAD) }
    }

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

            verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
            assertTrue(
                events.singleOrNull() is BackupEvent.Failed,
                "A cycle that recorded nothing must not be reported as a success: $events",
            )

            job.cancel()
        }

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

    // ── The destination disclosure gate (ADR 009 Decision 5, unit 3c) ───────────────

    /**
     * THE test of this gate. An automatic cycle is headless — it never passes through
     * `ProfileViewModel` — so a banner cannot satisfy "disclosed before the first upload"; only a
     * refusal to upload can. It is a refusal and not a failure: no streak, no reason, no watermark.
     */
    @Test
    fun `an undisclosed destination uploads nothing and books no failure`() = runTest(testDispatcher) {
        disclosed.clear()
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 0) { backupRepository.exportToJson(any(), any()) }
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }
        verify(exactly = 0) { metadata.setLastSuccessfulBackupAt(any(), any()) }
        verify(exactly = 0) { metadata.recordFailure(any(), any()) }
    }

    @Test
    fun `a manual tap hits the same gate as an automatic cycle`() = runTest(testDispatcher) {
        disclosed.clear()
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        orchestrator.requestBackup(manual = true)
        advanceUntilIdle()

        coVerify(exactly = 0) { backupRepository.exportToJson(any(), any()) }
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }
    }

    /**
     * The inverse of the test above, and it earns its place: a gate that never opens means backups
     * silently never run again, and nothing else in this repo would turn red for that.
     */
    @Test
    fun `acknowledging the destination lets the very next cycle upload`() = runTest(testDispatcher) {
        disclosed.clear()
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()

        backgroundFlow.emit(Unit)
        advanceUntilIdle()
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }

        orchestrator.acknowledgeDestination()
        advanceUntilIdle()

        verify(exactly = 1) { metadata.setDestinationDisclosed(USER_ID, NOW.toEpochMilliseconds()) }
        coVerify(exactly = 1) { uploader.upload(USER_ID, backupSnapshotName(NOW), PAYLOAD) }
    }

    @Test
    fun `acknowledging for one account leaves the next account undisclosed`() = runTest(testDispatcher) {
        disclosed.clear()
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        authenticate()
        orchestrator.acknowledgeDestination()
        advanceUntilIdle()

        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = OTHER_USER_ID, email = "b@b.com"))
        advanceUntilIdle()
        backgroundFlow.emit(Unit)
        advanceUntilIdle()

        coVerify(exactly = 1) { uploader.upload(USER_ID, any(), any()) }
        coVerify(exactly = 0) { uploader.upload(OTHER_USER_ID, any(), any()) }
    }

    @Test
    fun `acknowledging with no session writes nothing`() = runTest(testDispatcher) {
        disclosed.clear()

        val orchestrator = buildOrchestrator()
        orchestrator.start()
        sessionFlow.value = SessionStatus.NotAuthenticated
        advanceUntilIdle()

        orchestrator.acknowledgeDestination()
        advanceUntilIdle()

        verify(exactly = 0) { metadata.setDestinationDisclosed(any(), any()) }
        coVerify(exactly = 0) { uploader.upload(any(), any(), any()) }
    }

    private fun fixedClock(instant: Instant): Clock = object : Clock {
        override fun now(): Instant = instant
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-08-14T12:00:00Z")
        val CHANGED_AT: Long = Instant.parse("2026-08-14T10:00:00Z").toEpochMilliseconds()
        val DISCLOSED_AT: Long = Instant.parse("2026-08-01T00:00:00Z").toEpochMilliseconds()
        const val USER_ID = "uid-1"
        const val OTHER_USER_ID = "uid-2"
        const val APP_VERSION = "2.4.0"
        const val PRUNE_FAILED = "backup prune failed after a verified upload"
        const val CYCLE_FAILED = "backup cycle failed"
        const val OWNER_GONE = "backup upload finished for an account that is no longer signed in"
        const val OWNER_CHANGED = "Snapshot backup failed: the signed-in account changed"
        const val PAYLOAD = """{"schemaVersion":3}"""
        const val RETRY_BACKOFF_MILLIS = 6_000L
        const val UPLOAD_DURATION_MILLIS = 1_000L
    }
}
