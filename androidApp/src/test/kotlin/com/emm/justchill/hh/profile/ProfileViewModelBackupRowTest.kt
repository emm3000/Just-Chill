package com.emm.justchill.hh.profile

import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutResult
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.BackupFailureReason
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupStaleness
import com.emm.domain.shared.backup.GetBackupStalenessUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupEvent
import com.emm.justchill.core.backup.BackupHealth
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.SyncStatus
import com.emm.justchill.hh.shared.toMetaText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The "Último respaldo" row (ADR 009 Phase 3, unit 3b) — the mapping from
 * (session, `BackupHealth`, `isBackingUp`, staleness) onto [BackupRowUi].
 *
 * A sibling of [ProfileViewModelTest] rather than a section inside it, for the reason
 * [ProfileViewModelImportTest] already exists: that class carries every other concern the ViewModel
 * has and adding this one pushed it past detekt's `LargeClass` threshold. The fixture is duplicated
 * on purpose — a shared base class would couple two suites that are free to drift.
 */
class ProfileViewModelBackupRowTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val backupRepository = mockk<BackupRepository>(relaxed = true)
    private val importData = mockk<ImportDataUseCase>(relaxed = true)
    private val signOut = mockk<SignOutUseCase>(relaxed = true)
    private val deleteUserAccount = mockk<DeleteUserAccountUseCase>(relaxed = true)
    private val syncController = mockk<SyncController>(relaxed = true) {
        every { status } returns MutableStateFlow(SyncStatus())
    }

    private val backingUpFlow = MutableStateFlow(false)
    private val backupEvents = MutableSharedFlow<BackupEvent>(extraBufferCapacity = 4)
    private val healthFlow = MutableStateFlow(BackupHealth.None)
    private val backupController = mockk<BackupController>(relaxed = true) {
        every { isBackingUp } returns backingUpFlow
        every { events } returns backupEvents
        every { health } returns healthFlow
    }

    // Mocked rather than real: GetBackupStalenessUseCaseTest already owns the rule itself, and what
    // is under test here is the mapping from (session, health, staleness) to a BackupRowUi.
    private val getBackupStaleness = mockk<GetBackupStalenessUseCase>()
    private val logger = mockk<DiagnosticsLogger>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(emptyList())
    }
    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(emptyList())
    }

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
            syncController = syncController,
            backupController = backupController,
            getBackupStaleness = getBackupStaleness,
            logger = logger,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
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

        // BackupHealth.None publishes lastSuccessfulBackupAt == null for a signed-out device and for
        // a signed-in one that has never backed up. Those must not read the same on screen.
        assertEquals(BackupRowUi.NeedsAccount, vm.state.value.backupRow)
        coVerify(exactly = 0) { getBackupStaleness(any()) }
    }

    @Test
    fun `a signed-in device with no snapshot is Never`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        advanceUntilIdle()

        assertEquals(BackupRowUi.Never, vm.state.value.backupRow)
        coVerify(exactly = 0) { getBackupStaleness(any()) }
    }

    @Test
    fun `a running cycle wins over every branch below it`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } returns BackupStaleness(daysSinceLastBackup = 9, isStale = true)
        val vm = buildViewModel()

        // Signed in, a stale snapshot AND a failure streak — every other branch would claim this row.
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, consecutiveFailures = 2, BackupFailureReason.Network)
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
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.UpToDate(0), vm.state.value.backupRow)
    }

    @Test
    fun `the staleness answer is what turns an old snapshot into a warning`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(LAST_BACKUP_AT) } returns
            BackupStaleness(daysSinceLastBackup = 7, isStale = true)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.Stale(7), vm.state.value.backupRow)
    }

    @Test
    fun `an old snapshot the use case does not call stale stays UpToDate`() = runTest(testDispatcher) {
        // Same age as the test above, opposite verdict — so the row follows the rule and not the age.
        coEvery { getBackupStaleness(LAST_BACKUP_AT) } returns
            BackupStaleness(daysSinceLastBackup = 7, isStale = false)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.UpToDate(7), vm.state.value.backupRow)
    }

    @Test
    fun `a failure streak wins over the age, and carries the reason`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } returns BackupStaleness(daysSinceLastBackup = 1, isStale = false)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, consecutiveFailures = 1, BackupFailureReason.Network)
        advanceUntilIdle()

        assertEquals(BackupRowUi.Failed(BackupFailureReason.Network, lastBackupDaysAgo = 1), vm.state.value.backupRow)
    }

    /**
     * **The trap `BackupHealth`'s KDoc names.** `BackupFailureReason.fromNameOrNull` answers null for
     * a persisted reason name this build no longer has, so `(5, null)` is reachable on a device that
     * upgraded across a rename. A mapping keyed on `lastFailureReason != null` shows that device as
     * healthy — five failed cycles in a row, rendered as an up-to-date backup.
     */
    @Test
    fun `five failures with an unresolvable reason still warn`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } returns BackupStaleness(daysSinceLastBackup = 0, isStale = false)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, consecutiveFailures = 5, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.Failed(null, lastBackupDaysAgo = 0), vm.state.value.backupRow)
    }

    /**
     * The staleness read is the only database call on this path and it runs in a plain collector, so
     * an escaping exception would cancel `viewModelScope` and freeze the whole screen. The row has to
     * survive it AND the rest of the ViewModel has to keep working.
     */
    @Test
    fun `a database failure while reading staleness does not kill the screen`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } throws
            DomainException.DatabaseError(RuntimeException("disk I/O error"))
        coEvery { signOut.invoke() } returns SignOutResult.Revoked
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.Unreadable, vm.state.value.backupRow)
        verify { logger.warn(any(), any()) }

        // The other collectors are still alive: the sign-out intent still reaches its use case.
        vm.onIntent(ProfileIntent.SignOut)
        advanceUntilIdle()
        coVerify(exactly = 1) { signOut.invoke() }
    }

    /**
     * **The manual-tap regression.** `takeSnapshot` skips the due-check when `manual` is true, so an
     * automatic cycle can succeed at 09:00 and a tap on bad wifi fail at 10:00 — and then
     * `alreadyBackedUpOn` blocks every automatic cycle from clearing the streak until midnight. A
     * failure ranked above the snapshot would hide "backed up today" for the rest of the day, on a
     * device whose data is safe, under a row titled "Último respaldo".
     */
    @Test
    fun `a failure does not hide a snapshot taken today`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(LAST_BACKUP_AT) } returns
            BackupStaleness(daysSinceLastBackup = 0, isStale = false)
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, consecutiveFailures = 1, BackupFailureReason.Network)
        advanceUntilIdle()

        val row = vm.state.value.backupRow
        assertIs<BackupRowUi.Failed>(row)
        assertEquals(0, row.lastBackupDaysAgo, "The row must still be able to say the backup is from today.")
        assertEquals("Hoy · no pude actualizar", row.toMetaText())
    }

    /**
     * The other half of the same rule: failing with NOTHING backed up is the genuinely alarming case
     * and must stay distinguishable from the one above. Same streak, same reason — different sentence.
     */
    @Test
    fun `failing with no snapshot at all reads as no backup, not as a stale one`() = runTest(testDispatcher) {
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(
            lastSuccessfulBackupAt = null,
            consecutiveFailures = 1,
            lastFailureReason = BackupFailureReason.Network,
        )
        advanceUntilIdle()

        val row = vm.state.value.backupRow
        assertEquals(BackupRowUi.Failed(BackupFailureReason.Network, lastBackupDaysAgo = null), row)
        assertEquals("Sin respaldo · revisa tu conexión", row.toMetaText())
        // No watermark to age, so the database is never touched on this path.
        coVerify(exactly = 0) { getBackupStaleness(any()) }
    }

    /**
     * A cycle in flight when the session ends: the orchestrator's `isBackingUp` can still be true for
     * the moment it takes the gate to cancel it. "Respaldando…" would describe work being done for an
     * account the user just left.
     */
    @Test
    fun `a signed-out user is never told a backup is running`() = runTest(testDispatcher) {
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.NotAuthenticated)
        backingUpFlow.value = true
        advanceUntilIdle()

        assertEquals(BackupRowUi.NeedsAccount, vm.state.value.backupRow)
    }

    /**
     * The catch has to be broader than `DomainException`, and it was not at first.
     * `GetBackupStalenessUseCase` converts an epoch-millis watermark to a `LocalDate`, and
     * `kotlinx.datetime` raises `DateTimeArithmeticException` — not a `DomainException` — for a value
     * outside the representable range. Anything that escapes cancels `viewModelScope` and freezes the
     * whole screen, which is the exact outcome the containment exists to prevent.
     */
    @Test
    fun `a non-domain throwable while reading staleness is contained too`() = runTest(testDispatcher) {
        coEvery { getBackupStaleness(any()) } throws IllegalStateException("watermark out of range")
        coEvery { signOut.invoke() } returns SignOutResult.Revoked
        val vm = buildViewModel()

        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid", email = "a@b.com")))
        healthFlow.value = BackupHealth(LAST_BACKUP_AT, consecutiveFailures = 0, lastFailureReason = null)
        advanceUntilIdle()

        assertEquals(BackupRowUi.Unreadable, vm.state.value.backupRow)
        verify { logger.warn(any(), any()) }

        // The other collectors are still alive: the sign-out intent still reaches its use case.
        vm.onIntent(ProfileIntent.SignOut)
        advanceUntilIdle()
        coVerify(exactly = 1) { signOut.invoke() }
    }

    private companion object {
        /** 2026-08-04T15:04:05Z — a week before the fixed clock above. Any real instant would do. */
        const val LAST_BACKUP_AT: Long = 1_785_856_445_000L
    }
}
