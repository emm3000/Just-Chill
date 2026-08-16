package com.emm.justchill.hh.profile

import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutResult
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupRowCounts
import com.emm.domain.shared.backup.BackupVerification
import com.emm.domain.shared.backup.BackupVerifier
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
import com.emm.justchill.hh.shared.toText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val backupRepository = mockk<BackupRepository>()
    private val importData = mockk<ImportDataUseCase>(relaxed = true)
    private val signOut = mockk<SignOutUseCase>(relaxed = true)
    private val deleteUserAccount = mockk<DeleteUserAccountUseCase>(relaxed = true)
    private val syncController = mockk<SyncController>(relaxed = true) {
        every { status } returns MutableStateFlow(SyncStatus())
    }

    private val backingUpFlow = MutableStateFlow(false)
    private val backupEvents = MutableSharedFlow<BackupEvent>(extraBufferCapacity = 4)

    // Disclosed: this suite is about the ops, and an undisclosed destination refuses every one of
    // the manual taps below. The gate has its own tests.
    private val healthFlow = MutableStateFlow(BackupHealth.None.copy(canUploadToDestination = true))
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
    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(emptyList())
    }

    private val sessionFlow = MutableSharedFlow<SessionStatus>(replay = 1)
    private val observeSession = mockk<ObserveSessionUseCase>(relaxed = true)

    private val fixedNow = Instant.parse("2026-08-11T15:04:05Z")
    private val fixedClock = object : Clock {
        override fun now(): Instant = fixedNow
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
            backupVerifier = backupVerifier,
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
    fun `session emits Authenticated maps to SignedIn with email`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid1", email = "user@example.com")))
        advanceUntilIdle()

        val session = vm.state.value.session
        assertIs<SessionUiState.SignedIn>(session)
        assertEquals("user@example.com", session.email)
    }

    @Test
    fun `session emits Authenticated with null email maps to SignedIn with null email`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid2", email = null)))
        advanceUntilIdle()

        val session = vm.state.value.session
        assertIs<SessionUiState.SignedIn>(session)
        assertEquals(null, session.email)
    }

    @Test
    fun `session emits NotAuthenticated maps to SignedOut`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.NotAuthenticated)
        advanceUntilIdle()

        assertEquals(SessionUiState.SignedOut, vm.state.value.session)
    }

    @Test
    fun `SignOut intent invokes SignOutUseCase and emits SessionClosed notify effect on Revoked`() =
        runTest(testDispatcher) {
            coEvery { signOut.invoke() } returns SignOutResult.Revoked

            val vm = buildViewModel()
            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(ProfileIntent.SignOut)
            advanceUntilIdle()

            coVerify(exactly = 1) { signOut.invoke() }
            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.SessionClosed },
                "Expected SessionClosed notify not found in $effects",
            )

            job.cancel()
        }

    @Test
    fun `SignOut intent emits SessionClosedLocallyOnly notify effect on LocalOnly, not SessionClosed`() =
        runTest(testDispatcher) {
            coEvery { signOut.invoke() } returns SignOutResult.LocalOnly

            val vm = buildViewModel()
            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(ProfileIntent.SignOut)
            advanceUntilIdle()

            coVerify(exactly = 1) { signOut.invoke() }
            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.SessionClosedLocallyOnly },
                "Expected SessionClosedLocallyOnly notify not found in $effects",
            )
            assertTrue(
                effects.none { it is ProfileEffect.Notify && it.message == ProfileMessage.SessionClosed },
                "LocalOnly must not produce the SessionClosed (clean-revoke) notify: $effects",
            )

            job.cancel()
        }

    @Test
    fun `SignOut failure emits ShowError effect`() = runTest(testDispatcher) {
        coEvery { signOut.invoke() } throws DomainException.NetworkUnavailable(RuntimeException("no network"))

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.SignOut)
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.ShowError && it.error is DomainException.NetworkUnavailable },
            "Expected ShowError(NetworkUnavailable) not found in $effects",
        )

        job.cancel()
    }

    @Test
    fun `ExportRequested happy path emits ExportReady with the generated json`() = runTest(testDispatcher) {
        val expectedJson = """{"version":"1.0","data":[]}"""
        coEvery { backupRepository.exportToJson(any(), any()) } returns expectedJson

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ExportRequested)
        advanceUntilIdle()

        val exportReady = effects.filterIsInstance<ProfileEffect.ExportReady>().firstOrNull()
        assertEquals(expectedJson, exportReady?.json, "Expected ExportReady($expectedJson) not found in $effects")
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `ExportRequested stamps the injected app version into the backup`() = runTest(testDispatcher) {
        coEvery { backupRepository.exportToJson(any(), any()) } returns "{}"

        val vm = buildViewModel()

        vm.onIntent(ProfileIntent.ExportRequested)
        advanceUntilIdle()

        coVerify(exactly = 1) { backupRepository.exportToJson(any(), "1.0.0") }
    }

    @Test
    fun `ExportRequested stamps exportedAt from the injected clock`() = runTest(testDispatcher) {
        val exportedAt = slot<Long>()
        coEvery { backupRepository.exportToJson(capture(exportedAt), any()) } returns "{}"

        val vm = buildViewModel()

        vm.onIntent(ProfileIntent.ExportRequested)
        advanceUntilIdle()

        assertEquals(fixedNow.toEpochMilliseconds(), exportedAt.captured)
    }

    @Test
    fun `ExportRequested on DatabaseError emits ShowError effect`() = runTest(testDispatcher) {
        val cause = RuntimeException("db failure")
        coEvery { backupRepository.exportToJson(any(), any()) } throws DomainException.DatabaseError(cause)

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ExportRequested)
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.ShowError && it.error is DomainException.DatabaseError },
            "Expected ShowError(DatabaseError) not found in $effects",
        )
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `ExportRequested on Unknown error emits ShowError effect`() = runTest(testDispatcher) {
        coEvery { backupRepository.exportToJson(any(), any()) } throws
            DomainException.Unknown(RuntimeException("serialize error"))

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ExportRequested)
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.ShowError && it.error is DomainException.Unknown },
            "Expected ShowError(Unknown) not found in $effects",
        )
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `ExportRequested while import in flight is a no-op that reports OperationInProgress`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { importData(any()) } coAnswers {
                gate.await()
                error("unreachable")
            }

            val vm = buildViewModel()

            vm.onIntent(ProfileIntent.ImportJson("{}"))
            advanceUntilIdle()

            assertEquals(ProfileOp.Importing, vm.state.value.op)

            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(ProfileIntent.ExportRequested)
            advanceUntilIdle()

            assertTrue(
                effects.singleOrNull() == ProfileEffect.Notify(ProfileMessage.OperationInProgress),
                "Expected exactly one OperationInProgress notify, got: $effects",
            )

            gate.cancel()
            job.cancel()
        }

    @Test
    fun `ExportRequested re-fire while in flight emits OperationInProgress and does not re-invoke`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { backupRepository.exportToJson(any(), any()) } coAnswers {
                gate.await()
                ""
            }

            val vm = buildViewModel()
            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(ProfileIntent.ExportRequested)
            advanceUntilIdle()

            assertEquals(ProfileOp.Exporting, vm.state.value.op)

            vm.onIntent(ProfileIntent.ExportRequested)
            advanceUntilIdle()

            coVerify(exactly = 1) { backupRepository.exportToJson(any(), any()) }
            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.OperationInProgress },
                "Expected OperationInProgress notify not found in $effects",
            )

            gate.cancel()
            job.cancel()
        }

    @Test
    fun `DeleteAccount success emits AccountDeleted notify`() = runTest(testDispatcher) {
        coEvery { deleteUserAccount.invoke() } returns Unit

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.DeleteAccount)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteUserAccount.invoke() }
        assertTrue(
            effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.AccountDeleted },
            "Expected AccountDeleted notify not found in $effects",
        )

        job.cancel()
    }

    @Test
    fun `DeleteAccount failure emits ShowError and resets op`() = runTest(testDispatcher) {
        coEvery { deleteUserAccount.invoke() } throws DomainException.NetworkUnavailable(RuntimeException("no network"))

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.DeleteAccount)
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.ShowError && it.error is DomainException.NetworkUnavailable },
            "Expected ShowError(NetworkUnavailable) not found in $effects",
        )
        assertEquals(ProfileOp.None, vm.state.value.op, "op must reset after a failed deletion")

        job.cancel()
    }

    @Test
    fun `DeleteAccount re-fire while in flight emits OperationInProgress, ignores the re-fire, and resets op`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { deleteUserAccount.invoke() } coAnswers { gate.await() }

            val vm = buildViewModel()
            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(ProfileIntent.DeleteAccount)
            advanceUntilIdle()
            assertEquals(ProfileOp.DeletingAccount, vm.state.value.op, "op must be DeletingAccount while in flight")

            vm.onIntent(ProfileIntent.DeleteAccount)
            advanceUntilIdle()

            coVerify(exactly = 1) { deleteUserAccount.invoke() }
            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.OperationInProgress },
                "Expected OperationInProgress notify not found in $effects",
            )

            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(ProfileOp.None, vm.state.value.op, "op must reset after the delete completes")

            job.cancel()
        }

    @Test
    fun `lastSyncFailed=true from orchestrator maps to SyncRowUi-Failed`() = runTest(testDispatcher) {
        val statusFlow = MutableStateFlow(SyncStatus())
        every { syncController.status } returns statusFlow

        val vm = buildViewModel()
        advanceUntilIdle()

        statusFlow.value = SyncStatus(isSyncing = false, lastSyncFailed = true)
        advanceUntilIdle()

        assertIs<SyncRowUi.Failed>(vm.state.value.syncRow)
    }

    @Test
    fun `SyncRowUi-Failed resets to Idle when orchestrator emits successful status`() = runTest(testDispatcher) {
        val statusFlow = MutableStateFlow(SyncStatus(lastSyncFailed = true))
        every { syncController.status } returns statusFlow

        val vm = buildViewModel()
        advanceUntilIdle()

        assertIs<SyncRowUi.Failed>(vm.state.value.syncRow)

        statusFlow.value = SyncStatus(isSyncing = false, lastSyncedAtMillis = 1_000L, lastSyncFailed = false)
        advanceUntilIdle()

        val row = vm.state.value.syncRow
        assertIs<SyncRowUi.Idle>(row)
        assertEquals(1_000L, row.lastSyncedAtMillis)
    }

    @Test
    fun `isSyncing=true wins over lastSyncFailed=true producing SyncRowUi-Syncing`() = runTest(testDispatcher) {
        val statusFlow = MutableStateFlow(SyncStatus())
        every { syncController.status } returns statusFlow

        val vm = buildViewModel()
        advanceUntilIdle()

        statusFlow.value = SyncStatus(isSyncing = true, lastSyncFailed = true)
        advanceUntilIdle()

        assertIs<SyncRowUi.Syncing>(vm.state.value.syncRow)
    }

    private suspend fun signIn() {
        sessionFlow.emit(SessionStatus.Authenticated(AuthUser(userId = "uid-backup", email = "a@b.com")))
    }

    @Test
    fun `BackUpNow asks the orchestrator for a MANUAL cycle`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        signIn()
        advanceUntilIdle()

        vm.onIntent(ProfileIntent.BackUpNow)
        advanceUntilIdle()

        verify(exactly = 1) { backupController.requestBackup(manual = true) }
    }

    @Test
    fun `BackUpNow while another op is in flight reports OperationInProgress and asks for nothing`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { backupRepository.exportToJson(any(), any()) } coAnswers {
                gate.await()
                ""
            }

            val vm = buildViewModel()
            signIn()
            advanceUntilIdle()

            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(ProfileIntent.ExportRequested)
            advanceUntilIdle()
            assertEquals(ProfileOp.Exporting, vm.state.value.op)

            vm.onIntent(ProfileIntent.BackUpNow)
            advanceUntilIdle()

            verify(exactly = 0) { backupController.requestBackup(any()) }
            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.OperationInProgress },
                "Expected OperationInProgress notify not found in $effects",
            )

            gate.cancel()
            job.cancel()
        }

    @Test
    fun `BackUpNow while signed out is refused at the button and asks for nothing`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        sessionFlow.emit(SessionStatus.NotAuthenticated)
        advanceUntilIdle()

        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.BackUpNow)
        advanceUntilIdle()

        verify(exactly = 0) { backupController.requestBackup(any()) }
        assertTrue(
            effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.BackupNeedsAccount },
            "Expected BackupNeedsAccount notify not found in $effects",
        )

        job.cancel()
    }

    @Test
    fun `isBackingUp drives the op into BackingUp and back out`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(ProfileOp.None, vm.state.value.op)

        backingUpFlow.value = true
        advanceUntilIdle()
        assertEquals(ProfileOp.BackingUp, vm.state.value.op)

        backingUpFlow.value = false
        advanceUntilIdle()
        assertEquals(ProfileOp.None, vm.state.value.op, "The op must clear when the cycle ends")
    }

    @Test
    fun `an automatic backup starting mid-export does not steal the op slot`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { backupRepository.exportToJson(any(), any()) } coAnswers {
            gate.await()
            ""
        }

        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(ProfileIntent.ExportRequested)
        advanceUntilIdle()
        assertEquals(ProfileOp.Exporting, vm.state.value.op)

        backingUpFlow.value = true
        advanceUntilIdle()
        assertEquals(ProfileOp.Exporting, vm.state.value.op, "A backup must not claim a slot it did not take")

        backingUpFlow.value = false
        advanceUntilIdle()
        assertEquals(ProfileOp.Exporting, vm.state.value.op, "A backup must not release an op it never held")

        gate.cancel()
    }

    @Test
    fun `a successful backup cycle notifies BackupDone`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        backupEvents.emit(BackupEvent.Succeeded)
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.BackupDone },
            "Expected BackupDone notify not found in $effects",
        )

        job.cancel()
    }

    @Test
    fun `a failed backup notifies BackupFailed and never signs out or raises a session error`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()
            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }
            advanceUntilIdle()

            backupEvents.emit(BackupEvent.Failed(DomainException.Unauthorized("the signed-in account changed")))
            advanceUntilIdle()

            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.BackupFailed },
                "Expected BackupFailed notify not found in $effects",
            )
            assertTrue(
                effects.none { it is ProfileEffect.ShowError },
                "A backup failure must never become a ShowError — that is the effect toUserMessage() reads: $effects",
            )
            coVerify(exactly = 0) { signOut.invoke() }

            job.cancel()
        }

    @Test
    fun `an Unauthorized backup failure is not rendered as the expired-credentials message`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()
            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }
            advanceUntilIdle()

            backupEvents.emit(BackupEvent.Failed(DomainException.Unauthorized("the signed-in account changed")))
            advanceUntilIdle()

            val shown: String? = effects.filterIsInstance<ProfileEffect.Notify>().firstOrNull()?.message?.toText()
            assertEquals(
                "No pude respaldar en la nube — intenta de nuevo.",
                shown,
                "A backup failure must read as a backup failure",
            )

            job.cancel()
        }

    private fun TestScope.notifiedBy(vm: ProfileViewModel, intent: ProfileIntent): List<ProfileMessage> {
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        vm.onIntent(intent)
        advanceUntilIdle()

        job.cancel()
        return effects.filterIsInstance<ProfileEffect.Notify>().map { it.message }
    }

    @Test
    fun `VerifyBackup reports the snapshot that verified, naming the file and the counts it holds`() =
        runTest(testDispatcher) {
            coEvery { backupVerifier.verifyLatest() } returns VERIFIED_NEWEST

            val messages = notifiedBy(buildViewModel(), ProfileIntent.VerifyBackup)

            assertEquals(listOf(ProfileMessage.BackupVerified(VERIFIED_NEWEST)), messages)
            assertEquals(
                "Verificado: backup-v3-2026-08-16T14-22-08Z.json — 1 cuenta, 23 categorías, " +
                    "412 movimientos, 3 plantillas",
                messages.single().toText(),
            )
        }

    @Test
    fun `VerifyBackup that walked back to an older pair says so instead of reading as a plain success`() =
        runTest(testDispatcher) {
            coEvery { backupVerifier.verifyLatest() } returns VERIFIED_NEWEST.copy(isNewestPair = false)

            val shown: String = notifiedBy(buildViewModel(), ProfileIntent.VerifyBackup).single().toText()

            assertTrue(shown.startsWith("Verificado un respaldo más antiguo:"), shown)
        }

    @Test
    fun `VerifyBackup that found nothing to verify is not reported as pairs that failed`() = runTest(testDispatcher) {
        coEvery { backupVerifier.verifyLatest() } returns BackupVerification.NoSnapshots

        val messages = notifiedBy(buildViewModel(), ProfileIntent.VerifyBackup)

        assertEquals(listOf(ProfileMessage.BackupNotVerified(pairsInspected = 0)), messages)
    }

    @Test
    fun `VerifyBackup where no pair verified reports how many were inspected`() = runTest(testDispatcher) {
        coEvery { backupVerifier.verifyLatest() } returns BackupVerification.NothingVerified(pairsInspected = 5)

        val messages = notifiedBy(buildViewModel(), ProfileIntent.VerifyBackup)

        assertEquals(listOf(ProfileMessage.BackupNotVerified(pairsInspected = 5)), messages)
    }

    @Test
    fun `a failed verification notifies and never signs out or raises a session error`() = runTest(testDispatcher) {
        coEvery { backupVerifier.verifyLatest() } throws DomainException.Unauthorized("the session expired")

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        vm.onIntent(ProfileIntent.VerifyBackup)
        advanceUntilIdle()

        assertEquals(
            listOf<ProfileEffect>(ProfileEffect.Notify(ProfileMessage.BackupVerifyFailed)),
            effects.toList(),
            "A verification failure is a Notify — a ShowError would read as expired credentials",
        )
        coVerify(exactly = 0) { signOut.invoke() }
        assertEquals(ProfileOp.None, vm.state.value.op, "the op must clear after a failed verification")

        job.cancel()
    }

    @Test
    fun `VerifyBackup holds the op slot while it runs, and refuses a second tap`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<BackupVerification>()
        coEvery { backupVerifier.verifyLatest() } coAnswers { gate.await() }

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }
        advanceUntilIdle()

        vm.onIntent(ProfileIntent.VerifyBackup)
        advanceUntilIdle()
        assertEquals(ProfileOp.VerifyingBackup, vm.state.value.op)

        vm.onIntent(ProfileIntent.VerifyBackup)
        advanceUntilIdle()

        coVerify(exactly = 1) { backupVerifier.verifyLatest() }
        assertTrue(
            effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.OperationInProgress },
            "Expected OperationInProgress notify not found in $effects",
        )

        gate.complete(BackupVerification.NoSnapshots)
        advanceUntilIdle()
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `VerifyBackup while a backup cycle is running asks the verifier for nothing`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        signIn()
        backingUpFlow.value = true
        advanceUntilIdle()

        val messages = notifiedBy(vm, ProfileIntent.VerifyBackup)

        coVerify(exactly = 0) { backupVerifier.verifyLatest() }
        assertEquals(listOf(ProfileMessage.OperationInProgress), messages)
    }
}

private val VERIFIED_NEWEST = BackupVerification.Verified(
    fileName = "backup-v3-2026-08-16T14-22-08Z.json",
    takenAt = Instant.parse("2026-08-16T14:22:08Z"),
    schemaVersion = 3,
    rowCounts = BackupRowCounts(accounts = 1, categories = 23, transactions = 412, recurringMovements = 3),
    isNewestPair = true,
)
