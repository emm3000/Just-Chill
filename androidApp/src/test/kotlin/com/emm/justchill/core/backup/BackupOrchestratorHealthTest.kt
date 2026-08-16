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
import kotlinx.coroutines.cancel
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

    private fun TestScope.authenticate() {
        sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = USER_ID, email = "a@b.com"))
        advanceUntilIdle()
    }

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

    @Test
    fun `a verified success clears the streak and publishes the new watermark`() = runTest(testDispatcher) {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("no net"))
        failures[USER_ID] = BackupFailureState(4, BackupFailureReason.Serialization)
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

    @Test
    fun `a failure after an account switch is booked against the account that ran the cycle, not the new one`() =
        runTest(testDispatcher) {
            coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
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

            verify(exactly = 1) { metadata.recordFailure(USER_ID, BackupFailureReason.Unauthorized) }
            verify(exactly = 0) { metadata.recordFailure(OTHER_USER_ID, any()) }
            assertEquals(
                BackupHealth.None,
                orchestrator.health.value,
                "The account that just signed in must not inherit the previous one's failure",
            )
        }

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

    // InjectDispatcher: a real multithreaded dispatcher is the subject under test — the interleaving
    // this test forces cannot occur on a single-threaded test dispatcher.
    @Suppress("InjectDispatcher")
    @Test
    fun `a sign-out inside publishHealth leaves no stale account health`() = runBlocking {
        coEvery { backupRepository.latestLocalChangeAt() } returns CHANGED_AT
        coEvery { uploader.upload(any(), any(), any()) } throws
            DomainException.NetworkUnavailable(RuntimeException("no net"))

        lateinit var orchestrator: BackupOrchestrator
        var signedOutMidPublish = false
        every { metadata.lastSuccessfulBackupAt(USER_ID) } answers {
            if (failures.containsKey(USER_ID) && !signedOutMidPublish) {
                signedOutMidPublish = true
                sessionFlow.value = SessionStatus.NotAuthenticated
                awaitUntil("the sign-out to reach the orchestrator") {
                    orchestrator.health.value == BackupHealth.None
                }
            }
            LAST_SUCCESS
        }

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        try {
            orchestrator = BackupOrchestrator(
                backupRepository = backupRepository,
                uploader = uploader,
                pruner = pruner,
                metadata = metadata,
                observeSession = observeSession,
                appVersion = APP_VERSION,
                clock = fixedClock(NOW),
                timeZone = TimeZone.UTC,
                externalScope = scope,
                backgroundEvents = backgroundFlow,
                resumeEvents = resumeFlow,
                logger = logger,
            )
            orchestrator.start()
            sessionFlow.value = SessionStatus.Authenticated(AuthUser(userId = USER_ID, email = "a@b.com"))
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
        } finally {
            scope.cancel()
        }
    }

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
        const val OTHER_USER_ID = "uid-2"
        const val APP_VERSION = "2.4.0"
        val LAST_SUCCESS: Long = Instant.parse("2026-08-10T12:00:00Z").toEpochMilliseconds()
        const val CYCLE_FAILED = "backup cycle failed"
        const val STREAK_1 = "consecutive failures=1"
        const val STREAK_2 = "consecutive failures=2"
        const val AWAIT_TIMEOUT_MILLIS = 5_000L
        const val NANOS_PER_MILLI = 1_000_000L
        const val OWNER_CHANGED = "Snapshot backup failed: the signed-in account changed"
        const val PAYLOAD = """{"schemaVersion":3}"""
    }
}
