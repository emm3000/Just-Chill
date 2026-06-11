package com.emm.justchill.hh.profile

import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.AuthUser
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.ExportDataUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.sync.SyncOrchestrator
import com.emm.justchill.core.sync.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val exportData = mockk<ExportDataUseCase>()
    private val importData = mockk<ImportDataUseCase>(relaxed = true)
    private val signOut = mockk<SignOutUseCase>(relaxed = true)
    private val deleteUserAccount = mockk<DeleteUserAccountUseCase>(relaxed = true)
    private val syncOrchestrator = mockk<SyncOrchestrator>(relaxed = true) {
        every { status } returns MutableStateFlow(SyncStatus())
        every { events } returns MutableSharedFlow()
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

    private fun buildViewModel(): ProfileViewModel {
        every { observeSession.invoke() } returns sessionFlow
        return ProfileViewModel(
            exportData = exportData,
            importData = importData,
            signOut = signOut,
            deleteUserAccount = deleteUserAccount,
            syncOrchestrator = syncOrchestrator,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            observeSession = observeSession,
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
    fun `ExportToStream happy path writes json to stream and emits ExportDone`() = runTest(testDispatcher) {
        val expectedJson = """{"version":"1.0","data":[]}"""
        coEvery { exportData(any(), any()) } returns expectedJson

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        val outputStream = ByteArrayOutputStream()
        vm.onIntent(ProfileIntent.ExportToStream(outputStream))
        advanceUntilIdle()

        assertEquals(expectedJson, outputStream.toString(Charsets.UTF_8.name()))
        assertTrue(
            effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.ExportDone },
            "Expected ExportDone notify not found in $effects",
        )
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `ExportToStream on DatabaseError emits ShowError effect`() = runTest(testDispatcher) {
        val cause = RuntimeException("db failure")
        coEvery { exportData(any(), any()) } throws DomainException.DatabaseError(cause)

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        val outputStream = ByteArrayOutputStream()
        vm.onIntent(ProfileIntent.ExportToStream(outputStream))
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.ShowError && it.error is DomainException.DatabaseError },
            "Expected ShowError(DatabaseError) not found in $effects",
        )
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `ExportToStream on Unknown error emits ExportFailed notify`() = runTest(testDispatcher) {
        coEvery { exportData(any(), any()) } throws DomainException.Unknown(RuntimeException("io error"))

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ExportToStream(ByteArrayOutputStream()))
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.ExportFailed },
            "Expected ExportFailed notify not found in $effects",
        )
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `ExportToStream while import in flight is a no-op`() = runTest(testDispatcher) {
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

        vm.onIntent(ProfileIntent.ExportToStream(ByteArrayOutputStream()))
        advanceUntilIdle()

        assertTrue(effects.isEmpty(), "Export while import in flight must be a no-op, got: $effects")

        gate.cancel()
        job.cancel()
    }

    @Test
    fun `ExportToStream re-entry guard — second export while first in flight is a no-op`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { exportData(any(), any()) } coAnswers {
            gate.await()
            ""
        }

        val vm = buildViewModel()

        vm.onIntent(ProfileIntent.ExportToStream(ByteArrayOutputStream()))
        advanceUntilIdle() // suspended at gate — op == Exporting

        assertEquals(ProfileOp.Exporting, vm.state.value.op)

        vm.onIntent(ProfileIntent.ExportToStream(ByteArrayOutputStream()))
        advanceUntilIdle()

        // exportData must still have been called exactly once
        coVerify(exactly = 1) { exportData(any(), any()) }

        gate.cancel()
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

    // ── SyncRowUi mapping tests ────────────────────────────────────────────

    @Test
    fun `lastSyncFailed=true from orchestrator maps to SyncRowUi-Failed`() = runTest(testDispatcher) {
        val statusFlow = MutableStateFlow(SyncStatus())
        every { syncOrchestrator.status } returns statusFlow

        val vm = buildViewModel()
        advanceUntilIdle()

        statusFlow.value = SyncStatus(isSyncing = false, lastSyncFailed = true)
        advanceUntilIdle()

        assertIs<SyncRowUi.Failed>(vm.state.value.syncRow)
    }

    @Test
    fun `SyncRowUi-Failed resets to Idle when orchestrator emits successful status`() = runTest(testDispatcher) {
        val statusFlow = MutableStateFlow(SyncStatus(lastSyncFailed = true))
        every { syncOrchestrator.status } returns statusFlow

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
        every { syncOrchestrator.status } returns statusFlow

        val vm = buildViewModel()
        advanceUntilIdle()

        // Both flags true — Syncing must win (precedence rule lives in the ViewModel, not the composable).
        statusFlow.value = SyncStatus(isSyncing = true, lastSyncFailed = true)
        advanceUntilIdle()

        assertIs<SyncRowUi.Syncing>(vm.state.value.syncRow)
    }
}
