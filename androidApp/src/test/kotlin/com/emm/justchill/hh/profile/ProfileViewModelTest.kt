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
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupEvent
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

    // Real controllable flows, like sessionFlow below: every backup assertion in this file is about
    // what the ViewModel does with what the orchestrator publishes, so both have to be drivable.
    private val backingUpFlow = MutableStateFlow(false)
    private val backupEvents = MutableSharedFlow<BackupEvent>(extraBufferCapacity = 4)
    private val backupController = mockk<BackupController>(relaxed = true) {
        every { isBackingUp } returns backingUpFlow
        every { events } returns backupEvents
    }
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(emptyList())
    }
    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(emptyList())
    }

    // Real controllable flow — replaces the relaxed mock that returns empty flow.
    private val sessionFlow = MutableSharedFlow<SessionStatus>(replay = 1)
    private val observeSession = mockk<ObserveSessionUseCase>(relaxed = true)

    // The instant an export is stamped with. Stated here rather than read from the machine, which
    // is what makes the assertion below possible at all.
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
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            observeSession = observeSession,
            appVersion = "1.0.0",
            clock = fixedClock,
        )
    }

    // ── Session state tests ────────────────────────────────────────────────

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

    // ── Export tests ────────────────────────────────────────────────────────

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
        // The other half of the same stamp as the appVersion test above: what goes into the backup
        // as `exportedAt`. It had no test at all, which is reason enough for this one.
        //
        // What it does NOT prove — an earlier comment here claimed otherwise — is anything about
        // the deleted `= Clock.System` default. A Kotlin default never blocks an explicit argument,
        // so this exact test compiles and passes against the old constructor too. The default's
        // removal is a wiring guarantee (ProfileModule can no longer omit the clock without a
        // compile error), and nothing in this file exercises it. See docs/DATE_AUDIT.md #7.
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
        // The disk-space ExportFailed hint now lives in the platform write layer; the VM only
        // generates the JSON, so any domain failure here surfaces uniformly as ShowError.
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
            advanceUntilIdle() // suspended at gate — op == Importing

            assertEquals(ProfileOp.Importing, vm.state.value.op)

            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(ProfileIntent.ExportRequested)
            advanceUntilIdle()

            // The guard no longer swallows silently (docs/archive/sync/AUDIT.md §8) — it still runs no
            // export, but it now reports through the shared OperationInProgress notify.
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
            advanceUntilIdle() // suspended at gate — op == Exporting

            assertEquals(ProfileOp.Exporting, vm.state.value.op)

            vm.onIntent(ProfileIntent.ExportRequested)
            advanceUntilIdle()

            // exportData must still have been called exactly once
            coVerify(exactly = 1) { backupRepository.exportToJson(any(), any()) }
            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.OperationInProgress },
                "Expected OperationInProgress notify not found in $effects",
            )

            gate.cancel()
            job.cancel()
        }

    // ── DeleteAccount tests ────────────────────────────────────────────────

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

    /**
     * `docs/archive/sync/AUDIT.md` §8, candidate 1: the `launchOp` guard used to return silently on a
     * confirmed re-entry — indistinguishable on screen from the delete_account RPC never firing at
     * all. It now reports instead of swallowing, still invokes the use case exactly once, and
     * resets `op` once the in-flight call completes.
     */
    @Test
    fun `DeleteAccount re-fire while in flight emits OperationInProgress, ignores the re-fire, and resets op`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { deleteUserAccount.invoke() } coAnswers { gate.await() }

            val vm = buildViewModel()
            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(ProfileIntent.DeleteAccount)
            advanceUntilIdle() // first call is suspended at the gate
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

    // ── SyncRowUi mapping tests ────────────────────────────────────────────

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

        // Both flags true — Syncing must win (precedence rule lives in the ViewModel, not the composable).
        statusFlow.value = SyncStatus(isSyncing = true, lastSyncFailed = true)
        advanceUntilIdle()

        assertIs<SyncRowUi.Syncing>(vm.state.value.syncRow)
    }

    // ── Snapshot backup — the manual "Respaldar ahora" tap (ADR 009 2c-iv) ──

    /** Signs in and drains, because the tap is refused without a session. */
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

        // `manual = true` is the whole request: it is what skips the dirty check and the daily cap,
        // and what makes the cycle publish an outcome at all. An automatic request would leave the
        // user tapping a button that reports nothing and usually does nothing.
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

            // The existing guard is reused rather than bypassed: this is what keeps a tap from
            // racing an export or an import, and it must not reach the orchestrator at all.
            verify(exactly = 0) { backupController.requestBackup(any()) }
            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.OperationInProgress },
                "Expected OperationInProgress notify not found in $effects",
            )

            gate.cancel()
            job.cancel()
        }

    /**
     * `BackupOrchestrator.requestBackup` discards a session-less manual request rather than
     * deferring it, and reports nothing at all — so a tap that reached it while signed out would be
     * a button that silently does nothing. This ViewModel is the only place that sees both the tap
     * and the session, which is why the refusal lives here.
     */
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

    /**
     * `isBackingUp` reports EVERY cycle, and the automatic ones fire from the app's own lifecycle —
     * so one can start while an export the user asked for is still running. Overwriting the slot
     * would leave `launchOp`'s `finally` resetting it to None mid-backup, and the export's own
     * progress copy replaced by a backup nobody is waiting on.
     */
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

        // And releasing it is symmetric: the backup ending must not clear somebody else's op.
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

    /**
     * **The point of the whole unit.** No backup failure signs the user out and none reports a
     * session expiry, because the failure that would trigger both is a `DomainException.Unauthorized`
     * raised by the account-switch refusal — a user who is signed in, just as somebody else. The
     * negatives are asserted explicitly: a `ShowError` is the only effect that routes through
     * `toUserMessage()`, and `SignOutUseCase` is the only way this ViewModel can end a session.
     */
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

    /**
     * And the same failure seen from the words on screen, because that is where the damage would
     * land. `DomainExceptionExt.toUserMessage()` renders `Unauthorized` as the credentials/expiry
     * line, which is false for an account switch and alarming for a backup — the copy this ViewModel
     * picks has to be neither. Spelled out here rather than imported from `toUserMessage()`: a test
     * that builds its expectation out of the code under test agrees with any regression in it.
     */
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
            assertTrue(
                shown != CREDENTIALS_EXPIRED,
                "Unauthorized must not reach the user through the sign-in copy",
            )

            job.cancel()
        }

    private companion object {
        /** `DomainExceptionExt.toUserMessage()`'s Unauthorized branch, spelled out on purpose. */
        const val CREDENTIALS_EXPIRED = "Credenciales incorrectas o sesión expirada"
    }
}
