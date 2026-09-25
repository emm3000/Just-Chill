package com.emm.justchill.feature.profile

import com.emm.justchill.core.domain.auth.GetSessionStatusUseCase
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.recurring.GetRecurringMonthlySummaryUseCase
import com.emm.justchill.core.domain.recurring.RecurringMonthlySummary
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.backup.BackupController
import com.emm.justchill.core.domain.shared.backup.BackupHealth
import com.emm.justchill.core.domain.shared.backup.ExportHistory
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.transaction.ExportTransactionsCsvUseCase
import com.emm.justchill.core.domain.transaction.TransactionsCsv
import com.emm.justchill.core.domain.transaction.TransactionsCsvScope
import com.emm.justchill.core.testing.FakeBackupAvailability
import com.emm.justchill.core.testing.FakeTodayFlow
import com.emm.justchill.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Clock
import kotlin.time.Instant

class ProfileViewModelCsvExportTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val monthCsv = TransactionsCsv(
        fileName = "justchill-movimientos-2026-09.csv",
        content = "\uFEFFfecha,tipo,monto,cuenta,categoría,nota\r\n2026-09-25,Gasto,12.50,Efectivo,Comida,menú\r\n",
    )
    private val exportTransactionsCsv: ExportTransactionsCsvUseCase = mockk {
        coEvery { this@mockk.invoke(TransactionsCsvScope.CurrentMonth) } returns monthCsv
    }
    private val localExportHistory: ExportHistory = mockk(relaxed = true) {
        every { daysSinceLastExport(any()) } returns null
    }
    private val backupController: BackupController = mockk(relaxed = true) {
        every { isBackingUp } returns MutableStateFlow(false)
        every { events } returns emptyFlow()
        every { health } returns MutableStateFlow(BackupHealth.None)
    }
    private val categoryRepository: CategoryRepository = mockk {
        every { all() } returns flowOf(emptyList())
    }
    private val getRecurringMonthlySummary: GetRecurringMonthlySummaryUseCase = mockk {
        every { this@mockk.invoke() } returns flowOf(
            RecurringMonthlySummary(activeCount = 0, monthlyOutflow = Money.Zero),
        )
    }
    private val getSessionStatus: GetSessionStatusUseCase = mockk {
        every { this@mockk.invoke() } returns emptyFlow()
    }
    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-09-25T15:00:00Z")
    }

    private fun buildViewModel(): ProfileViewModel = ProfileViewModel(
        backupRepository = mockk(relaxed = true),
        importData = mockk(relaxed = true),
        signOut = mockk(relaxed = true),
        deleteUserAccount = mockk(relaxed = true),
        backupController = backupController,
        backupVerifier = mockk(),
        getBackupStaleness = mockk(relaxed = true),
        logger = mockk(relaxed = true),
        categoryRepository = categoryRepository,
        localExportHistory = localExportHistory,
        todayFlow = FakeTodayFlow(MutableStateFlow(LocalDate(2026, 9, 25))),
        getRecurringMonthlySummary = getRecurringMonthlySummary,
        getSessionStatus = getSessionStatus,
        backupAvailability = FakeBackupAvailability(isAvailable = false),
        exportTransactionsCsv = exportTransactionsCsv,
        appVersion = "1.0.0",
        clock = fixedClock,
    )

    private fun TestScope.recordEffects(vm: ProfileViewModel, into: MutableList<ProfileEffect>): Job =
        launch { vm.effect.collect(into::add) }

    @Test
    fun `tapping export opens the export sheet`() = runTest(testDispatcher) {
        val vm: ProfileViewModel = buildViewModel()

        vm.onIntent(ProfileIntent.ExportClicked)

        assertEquals(ProfileDialog.Export, vm.state.value.dialog)
    }

    @Test
    fun `a CSV row emits one effect carrying the file name and the content`() = runTest(testDispatcher) {
        val vm: ProfileViewModel = buildViewModel()
        val effects: MutableList<ProfileEffect> = mutableListOf()
        val recording: Job = recordEffects(vm, effects)
        vm.onIntent(ProfileIntent.ExportClicked)

        vm.onIntent(ProfileIntent.CsvExportRequested(TransactionsCsvScope.CurrentMonth))
        advanceUntilIdle()

        assertEquals(listOf<ProfileEffect>(ProfileEffect.CsvReady(monthCsv.fileName, monthCsv.content)), effects)
        recording.cancel()
    }

    @Test
    fun `the CSV content never lands in state`() = runTest(testDispatcher) {
        val vm: ProfileViewModel = buildViewModel()
        val recording: Job = recordEffects(vm, mutableListOf())
        vm.onIntent(ProfileIntent.ExportClicked)

        vm.onIntent(ProfileIntent.CsvExportRequested(TransactionsCsvScope.CurrentMonth))
        advanceUntilIdle()

        assertFalse(vm.state.value.toString().contains(monthCsv.content))
        assertEquals(ProfileDialog.None, vm.state.value.dialog)
        assertEquals(ProfileOp.None, vm.state.value.op)
        recording.cancel()
    }

    @Test
    fun `a CSV export never records the backup export watermark`() = runTest(testDispatcher) {
        val vm: ProfileViewModel = buildViewModel()
        val recording: Job = recordEffects(vm, mutableListOf())

        vm.onIntent(ProfileIntent.CsvExportRequested(TransactionsCsvScope.CurrentMonth))
        advanceUntilIdle()

        coVerify(exactly = 1) { exportTransactionsCsv(TransactionsCsvScope.CurrentMonth) }
        verify(exactly = 0) { localExportHistory.recordExport() }
        recording.cancel()
    }

    @Test
    fun `a failed share tells the user the export did not go out`() = runTest(testDispatcher) {
        val vm: ProfileViewModel = buildViewModel()
        val effects: MutableList<ProfileEffect> = mutableListOf()
        val recording: Job = recordEffects(vm, effects)

        vm.onIntent(ProfileIntent.CsvShareFailed)
        advanceUntilIdle()

        assertEquals(listOf<ProfileEffect>(ProfileEffect.Notify(ProfileMessage.CsvExportFailed)), effects)
        recording.cancel()
    }

    @Test
    fun `a CSV that could not be built tells the user the same way`() = runTest(testDispatcher) {
        coEvery { exportTransactionsCsv(TransactionsCsvScope.Everything) } throws
            DomainException.DatabaseError(IllegalStateException("database closed"))
        val vm: ProfileViewModel = buildViewModel()
        val effects: MutableList<ProfileEffect> = mutableListOf()
        val recording: Job = recordEffects(vm, effects)

        vm.onIntent(ProfileIntent.CsvExportRequested(TransactionsCsvScope.Everything))
        advanceUntilIdle()

        assertEquals(listOf<ProfileEffect>(ProfileEffect.Notify(ProfileMessage.CsvExportFailed)), effects)
        assertEquals(ProfileOp.None, vm.state.value.op)
        recording.cancel()
    }

    @Test
    fun `the sheet stays open and busy while the CSV is being built`() = runTest(testDispatcher) {
        val gate: CompletableDeferred<TransactionsCsv> = CompletableDeferred()
        coEvery { exportTransactionsCsv(TransactionsCsvScope.CurrentMonth) } coAnswers { gate.await() }
        val vm: ProfileViewModel = buildViewModel()
        val recording: Job = recordEffects(vm, mutableListOf())
        vm.onIntent(ProfileIntent.ExportClicked)

        vm.onIntent(ProfileIntent.CsvExportRequested(TransactionsCsvScope.CurrentMonth))
        advanceUntilIdle()

        assertEquals(ProfileDialog.Export, vm.state.value.dialog)
        assertEquals(ProfileOp.Exporting, vm.state.value.op)
        gate.complete(monthCsv)
        advanceUntilIdle()
        recording.cancel()
    }
}
