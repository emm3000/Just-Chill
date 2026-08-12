package com.emm.justchill.hh.profile

import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
    fun `SignOut intent invokes SignOutUseCase and emits SessionClosed notify effect`() = runTest(testDispatcher) {
        coEvery { signOut.invoke() } returns Unit

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

            // The guard no longer swallows silently (docs/sync/AUDIT.md §8) — it still runs no
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

    @Test
    fun `DeleteAccount re-fire while in flight is ignored`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { deleteUserAccount.invoke() } coAnswers { gate.await() }

        val vm = buildViewModel()

        vm.onIntent(ProfileIntent.DeleteAccount)
        advanceUntilIdle() // first call is suspended at the gate
        assertEquals(ProfileOp.DeletingAccount, vm.state.value.op, "op must be DeletingAccount while in flight")

        vm.onIntent(ProfileIntent.DeleteAccount)
        advanceUntilIdle()

        coVerify(exactly = 1) { deleteUserAccount.invoke() }

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(ProfileOp.None, vm.state.value.op, "op must reset after the delete completes")
    }

    /**
     * `docs/sync/AUDIT.md` §8, candidate 1: the `launchOp` guard used to return silently on a
     * confirmed re-entry — indistinguishable on screen from the delete_account RPC never firing at
     * all. It now reports instead of swallowing.
     */
    @Test
    fun `DeleteAccount re-fire while in flight emits OperationInProgress and does not re-invoke`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { deleteUserAccount.invoke() } coAnswers { gate.await() }

            val vm = buildViewModel()
            val effects = mutableListOf<ProfileEffect>()
            val job = launch { vm.effect.collect { effects.add(it) } }

            vm.onIntent(ProfileIntent.DeleteAccount)
            advanceUntilIdle() // first call is suspended at the gate

            vm.onIntent(ProfileIntent.DeleteAccount)
            advanceUntilIdle()

            coVerify(exactly = 1) { deleteUserAccount.invoke() }
            assertTrue(
                effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.OperationInProgress },
                "Expected OperationInProgress notify not found in $effects",
            )

            gate.cancel()
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
}
