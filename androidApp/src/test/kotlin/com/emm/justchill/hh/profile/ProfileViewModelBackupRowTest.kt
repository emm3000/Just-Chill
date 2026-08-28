package com.emm.justchill.hh.profile

import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutResult
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.recurring.GetRecurringMonthlySummaryUseCase
import com.emm.domain.recurring.RecurringMonthlySummary
import com.emm.domain.shared.Money
import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupStaleness
import com.emm.domain.shared.backup.BackupVerifier
import com.emm.domain.shared.backup.GetBackupStalenessUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupEvent
import com.emm.justchill.core.backup.BackupHealth
import com.emm.justchill.core.backup.LocalExportHistory
import com.emm.justchill.core.time.FakeTodayFlow
import com.emm.justchill.hh.shared.toMetaText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

// The fixture is duplicated with ProfileViewModelTest on purpose — a shared base class would
// couple two suites that are free to drift.
class ProfileViewModelBackupRowTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val backupRepository = mockk<BackupRepository>(relaxed = true)
    private val importData = mockk<ImportDataUseCase>(relaxed = true)
    private val signOut = mockk<SignOutUseCase>(relaxed = true)
    private val deleteUserAccount = mockk<DeleteUserAccountUseCase>(relaxed = true)

    private val backingUpFlow = MutableStateFlow(false)
    private val backupEvents = MutableSharedFlow<BackupEvent>(extraBufferCapacity = 4)
    private val healthFlow = MutableStateFlow(BackupHealth.None)
    private val backupController = mockk<BackupController>(relaxed = true) {
        every { isBackingUp } returns backingUpFlow
        every { events } returns backupEvents
        every { health } returns healthFlow
    }

    private val backupVerifier = mockk<BackupVerifier>()

    private val getBackupStaleness = mockk<GetBackupStalenessUseCase>()
    private val logger = mockk<DiagnosticsLogger>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(emptyList())
    }
    private val getRecurringMonthlySummary = mockk<GetRecurringMonthlySummaryUseCase> {
        every { this@mockk.invoke() } returns
            flowOf(RecurringMonthlySummary(activeCount = 0, monthlyOutflow = Money.Zero))
    }
    private val localExportHistory = mockk<LocalExportHistory>(relaxed = true) {
        every { daysSinceLastExport(any()) } returns null
    }
    private val todayFlow = FakeTodayFlow(MutableStateFlow(LocalDate(2026, 8, 28)))

    private val sessionFlow = MutableSharedFlow<SessionStatus>(replay = 1)
    private val observeSession = mockk<ObserveSessionUseCase>(relaxed = true)

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-11T15:04:05Z")
    }

    private fun buildViewModel(): ProfileViewModel {
        every { observeSession.invoke() } returns sessionFlow
        return ProfileViewModel(
            backupRepository = backupRepository,
            importData = importData,
            signOut = signOut,
            deleteUserAccount = deleteUserAccount,
            backupController = backupController,
            backupVerifier = backupVerifier,
            getBackupStaleness = getBackupStaleness,
            logger = logger,
            categoryRepository = categoryRepository,
            localExportHistory = localExportHistory,
            todayFlow = todayFlow,
            getRecurringMonthlySummary = getRecurringMonthlySummary,
            observeSession = observeSession,
            appVersion = "1.0.0",
            clock = fixedClock,
        )
    }

    @Test
    fun `before anything is observed the row does not claim a healthy backup`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(BackupRowUi.NeedsAccount, vm.state.value.backupRow)
    }

    @Test
    fun `a signed-out device is NeedsAccount, not Never`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.NotAuthenticated)
        advanceUntilIdle()

        assertEquals(BackupRowUi.NeedsAccount, vm.state.value.backupRow)
        coVerify(exactly = 0) { getBackupStaleness(any()) }
    }

    @Test
    fun `a signed-in device with no snapshot is Never`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(null, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.Never, vm.state.value.backupRow)
        coVerify(exactly = 0) { getBackupStaleness(any()) }
    }

    @Test
    fun `a running cycle wins over every branch below it`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } returns BackupStaleness(daysSinceLastBackup = 9, isStale = true)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 2, BackupFailureReason.Network)
        backingUpFlow.value = true
        advanceUntilIdle()

        assertEquals(BackupRowUi.BackingUp, vm.state.value.backupRow)
    }

    @Test
    fun `a fresh snapshot with no failures is UpToDate, carrying the day count`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(LAST_BACKUP_AT) } returns
            BackupStaleness(daysSinceLastBackup = 0, isStale = false)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.UpToDate(0), vm.state.value.backupRow)
    }

    @Test
    fun `the staleness answer is what turns an old snapshot into a warning`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(LAST_BACKUP_AT) } returns
            BackupStaleness(daysSinceLastBackup = 7, isStale = true)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.Stale(7), vm.state.value.backupRow)
    }

    @Test
    fun `an old snapshot the use case does not call stale stays UpToDate`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(LAST_BACKUP_AT) } returns
            BackupStaleness(daysSinceLastBackup = 7, isStale = false)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.UpToDate(7), vm.state.value.backupRow)
    }

    @Test
    fun `a failure streak wins over the age, and carries the reason`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } returns BackupStaleness(daysSinceLastBackup = 1, isStale = false)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 1, BackupFailureReason.Network)
        advanceUntilIdle()

        assertEquals(
            BackupRowUi.Failed(BackupFailureReason.Network, LastSnapshot.DaysAgo(days = 1, isStale = false)),
            vm.state.value.backupRow,
        )
    }

    @Test
    fun `five failures with an unresolvable reason still warn`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } returns BackupStaleness(daysSinceLastBackup = 0, isStale = false)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 5, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(
            BackupRowUi.Failed(null, LastSnapshot.DaysAgo(days = 0, isStale = false)),
            vm.state.value.backupRow,
        )
    }

    @Test
    fun `a database failure while reading staleness does not kill the screen`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } throws
            DomainException.DatabaseError(RuntimeException("disk I/O error"))
        coEvery { signOut.invoke() } returns SignOutResult.Revoked
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.Unreadable, vm.state.value.backupRow)
        verify { logger.warn(any(), any()) }

        vm.onIntent(ProfileIntent.SignOut)
        advanceUntilIdle()
        coVerify(exactly = 1) { signOut.invoke() }
    }

    @Test
    fun `a failure does not hide a snapshot taken today`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(LAST_BACKUP_AT) } returns
            BackupStaleness(daysSinceLastBackup = 0, isStale = false)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 1, BackupFailureReason.Network)
        advanceUntilIdle()

        val row = vm.state.value.backupRow
        assertIs<BackupRowUi.Failed>(row)
        assertEquals(
            LastSnapshot.DaysAgo(days = 0, isStale = false),
            row.lastSnapshot,
            "The row must still be able to say the backup is from today.",
        )
        assertEquals("Hoy · revisa tu conexión", row.toMetaText())
        assertEquals(BackupRowSeverity.Warning, row.severity())
    }

    @Test
    fun `failing with no snapshot at all reads as no backup, not as a stale one`() = runTest(testDispatcher) {
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(
            lastSuccessfulBackupAt = null,
            consecutiveFailures = 1,
            lastFailureReason = BackupFailureReason.Network,
        )
        advanceUntilIdle()

        val row = vm.state.value.backupRow
        assertEquals(BackupRowUi.Failed(BackupFailureReason.Network, LastSnapshot.None), row)
        assertEquals("Sin respaldo · revisa tu conexión", row.toMetaText())
        coVerify(exactly = 0) { getBackupStaleness(any()) }
    }

    @Test
    fun `a signed-out user is never told a backup is running`() = runTest(testDispatcher) {
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.NotAuthenticated)
        backingUpFlow.value = true
        advanceUntilIdle()

        assertEquals(BackupRowUi.NeedsAccount, vm.state.value.backupRow)
    }

    @Test
    fun `a non-domain throwable while reading staleness is contained too`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } throws IllegalStateException("watermark out of range")
        coEvery { signOut.invoke() } returns SignOutResult.Revoked
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.Unreadable, vm.state.value.backupRow)
        verify { logger.warn(any(), any()) }

        vm.onIntent(ProfileIntent.SignOut)
        advanceUntilIdle()
        coVerify(exactly = 1) { signOut.invoke() }
    }

    @Test
    fun `a failing device whose staleness read throws keeps the failure`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } throws IllegalStateException("watermark out of range")
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 5, BackupFailureReason.Unauthorized)
        advanceUntilIdle()

        val row = vm.state.value.backupRow
        assertEquals(BackupRowUi.Failed(BackupFailureReason.Unauthorized, LastSnapshot.AgeUnknown), row)
        assertEquals("No pude respaldar · vuelve a iniciar sesión", row.toMetaText())
        verify { logger.warn(any(), any()) }
    }

    @Test
    fun `a stale snapshot under a failure streak escalates and names the action`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(LAST_BACKUP_AT) } returns
            BackupStaleness(daysSinceLastBackup = 30, isStale = true)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = health(LAST_BACKUP_AT, consecutiveFailures = 12, BackupFailureReason.Unauthorized)
        advanceUntilIdle()

        val row = vm.state.value.backupRow
        assertEquals(
            BackupRowUi.Failed(BackupFailureReason.Unauthorized, LastSnapshot.DaysAgo(days = 30, isStale = true)),
            row,
        )
        assertEquals("Hace 30 días · vuelve a iniciar sesión", row.toMetaText())
        assertEquals(BackupRowSeverity.Danger, row.severity())
    }

    // ── The destination disclosure (ADR 009 Decision 5, unit 3c) ───────────────────

    @Test
    fun `a signed-in device whose destination was never disclosed asks before anything else`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()

            sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
            healthFlow.value = BackupHealth(LAST_BACKUP_AT, 0, null, canUploadToDestination = false)
            advanceUntilIdle()

            assertEquals(BackupRowUi.DisclosurePending, vm.state.value.backupRow)
            coVerify(exactly = 0) { getBackupStaleness(any()) }
        }

    /**
     * The orchestrator raises `isBackingUp` for the cycle it is about to refuse, so a lower rank
     * would flash "Respaldando…" over a device uploading nothing.
     */
    @Test
    fun `a pending disclosure outranks a running cycle`() = runTest(testDispatcher) {
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, 0, null, canUploadToDestination = false)
        backingUpFlow.value = true
        advanceUntilIdle()

        assertEquals(BackupRowUi.DisclosurePending, vm.state.value.backupRow)
    }

    @Test
    fun `a signed-out device is still NeedsAccount, never a disclosure it has nobody to make to`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()

            sessionFlow.emit(SessionStatus.NotAuthenticated)
            healthFlow.value = BackupHealth(null, 0, null, canUploadToDestination = false)
            advanceUntilIdle()

            assertEquals(BackupRowUi.NeedsAccount, vm.state.value.backupRow)
        }

    @Test
    fun `acknowledging while idle writes the flag and requests the cycle`() = runTest(testDispatcher) {
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, 0, null, canUploadToDestination = false)
        advanceUntilIdle()

        vm.onIntent(ProfileIntent.AcknowledgeBackupDestination)
        advanceUntilIdle()

        verify(exactly = 1) { backupController.acknowledgeDestination(requestCycle = true) }
    }

    @Test
    fun `acknowledging during a non-idle op writes the flag and requests no cycle`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { backupRepository.exportToJson(any(), any()) } coAnswers {
            gate.await()
            ""
        }

        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, 0, null, canUploadToDestination = false)
        advanceUntilIdle()

        vm.onIntent(ProfileIntent.ExportRequested)
        advanceUntilIdle()
        assertEquals(ProfileOp.Exporting, vm.state.value.op)

        vm.onIntent(ProfileIntent.AcknowledgeBackupDestination)
        advanceUntilIdle()

        verify(exactly = 1) { backupController.acknowledgeDestination(requestCycle = false) }
        verify(exactly = 0) { backupController.requestBackup(any<Boolean>()) }

        gate.cancel()
    }

    @Test
    fun `tapping Respaldar ahora while the disclosure is pending says so instead of doing nothing`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()

            sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
            healthFlow.value = BackupHealth(LAST_BACKUP_AT, 0, null, canUploadToDestination = false)
            advanceUntilIdle()

            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }
            advanceUntilIdle()

            vm.onIntent(ProfileIntent.BackUpNow)
            advanceUntilIdle()

            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.BackupNeedsDisclosure },
                "Expected BackupNeedsDisclosure notify not found in $effects",
            )
            verify(exactly = 0) { backupController.requestBackup(any<Boolean>()) }

            job.cancel()
        }

    // Every case below describes a destination the user already acknowledged; the pending
    // disclosure outranks all of them and has its own tests.
    private fun health(
        lastSuccessfulBackupAt: Long?,
        consecutiveFailures: Int,
        lastFailureReason: BackupFailureReason?,
    ) = BackupHealth(lastSuccessfulBackupAt, consecutiveFailures, lastFailureReason, canUploadToDestination = true)

    private companion object {
        const val LAST_BACKUP_AT: Long = 1_785_856_445_000L
    }
}
