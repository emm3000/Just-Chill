package com.emm.justchill.hh.profile

import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.GetBackupStalenessUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.backup.ImportStats
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupHealth
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.SyncStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class ProfileViewModelImportTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val backupRepository = mockk<BackupRepository>(relaxed = true)
    private val importData = mockk<ImportDataUseCase>()
    private val signOut = mockk<SignOutUseCase>(relaxed = true)
    private val deleteUserAccount = mockk<DeleteUserAccountUseCase>(relaxed = true)
    private val syncController = mockk<SyncController>(relaxed = true) {
        every { status } returns MutableStateFlow(SyncStatus())
    }
    private val backupController = mockk<BackupController>(relaxed = true) {
        every { isBackingUp } returns MutableStateFlow(false)
        every { events } returns emptyFlow()
        every { health } returns MutableStateFlow(BackupHealth.None)
    }
    private val getBackupStaleness = mockk<GetBackupStalenessUseCase>(relaxed = true)
    private val observeSession = mockk<ObserveSessionUseCase>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns flowOf(emptyList())
    }
    private val accountRepository = mockk<AccountRepository> {
        every { all() } returns flowOf(emptyList())
    }

    // Nothing here asserts the export stamp — that lives in ProfileViewModelTest — but the clock is
    // still stated rather than read: no test in this repo should reintroduce the ambient default
    // the constructor just lost.
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-11T15:04:05Z")
    }

    private fun buildViewModel() = ProfileViewModel(
        backupRepository = backupRepository,
        importData = importData,
        signOut = signOut,
        deleteUserAccount = deleteUserAccount,
        syncController = syncController,
        backupController = backupController,
        getBackupStaleness = getBackupStaleness,
        categoryRepository = categoryRepository,
        accountRepository = accountRepository,
        observeSession = observeSession,
        appVersion = "1.0.0",
        clock = fixedClock,
    )

    @Test
    fun `ImportJson happy path emits ImportDone with both counts`() = runTest(testDispatcher) {
        coEvery { importData(any()) } returns
            ImportStats(accounts = 2, categories = 5, transactions = 234, recurring = 6)

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ImportJson("{}"))
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.ImportDone(234, 6) },
            "Expected ImportDone(234, 6) notify not found in $effects",
        )
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `ImportJson on ValidationError emits ShowError with the domain exception`() = runTest(testDispatcher) {
        coEvery { importData(any()) } throws DomainException.ValidationError("Archivo corrupto")

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ImportJson("{ bad }"))
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.ShowError && it.error is DomainException.ValidationError },
            "Expected ShowError(ValidationError) not found in $effects",
        )
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `ImportJson on non-ValidationError emits ImportFailed notify`() = runTest(testDispatcher) {
        coEvery { importData(any()) } throws DomainException.DatabaseError(RuntimeException("db"))

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ImportJson("{}"))
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.Notify && it.message == ProfileMessage.ImportFailed },
            "Expected ImportFailed notify not found in $effects",
        )
        assertEquals(ProfileOp.None, vm.state.value.op)

        job.cancel()
    }

    @Test
    fun `ImportJson resets op to None after completion`() = runTest(testDispatcher) {
        coEvery { importData(any()) } returns
            ImportStats(accounts = 1, categories = 1, transactions = 10, recurring = 0)

        val vm = buildViewModel()

        vm.onIntent(ProfileIntent.ImportJson("{}"))
        advanceUntilIdle()

        assertEquals(ProfileOp.None, vm.state.value.op)
    }

    @Test
    fun `ImportJson while export in flight is a no-op that reports OperationInProgress`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { backupRepository.exportToJson(any(), any()) } coAnswers {
            gate.await()
            ""
        }

        val vm = buildViewModel()

        vm.onIntent(ProfileIntent.ExportRequested)
        advanceUntilIdle() // suspended at gate — op == Exporting

        assertEquals(ProfileOp.Exporting, vm.state.value.op)

        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ImportJson("{}"))
        advanceUntilIdle()

        // The guard no longer swallows silently (docs/archive/sync/AUDIT.md §8) — it still runs no import,
        // but it now reports through the shared OperationInProgress notify.
        assertTrue(
            effects.singleOrNull() == ProfileEffect.Notify(ProfileMessage.OperationInProgress),
            "Expected exactly one OperationInProgress notify, got: $effects",
        )

        gate.cancel()
        job.cancel()
    }
}
