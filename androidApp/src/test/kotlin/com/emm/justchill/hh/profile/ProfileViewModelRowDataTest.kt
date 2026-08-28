package com.emm.justchill.hh.profile

import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.SessionStatus
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.recurring.GetRecurringMonthlySummaryUseCase
import com.emm.domain.recurring.RecurringMonthlySummary
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.shared.backup.BackupVerifier
import com.emm.domain.shared.backup.GetBackupStalenessUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.logging.DiagnosticsLogger
import com.emm.justchill.MainDispatcherRule
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupEvent
import com.emm.justchill.core.backup.BackupHealth
import com.emm.justchill.core.backup.LocalExportHistory
import com.emm.justchill.core.time.FakeTodayFlow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

// What the Gestionar and Respaldo rows say about the ledger: the category split, the recurring
// summary and the age of the last export.
class ProfileViewModelRowDataTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val backupRepository = mockk<BackupRepository>()

    private val backupController = mockk<BackupController>(relaxed = true) {
        every { isBackingUp } returns MutableStateFlow(false)
        every { events } returns MutableSharedFlow<BackupEvent>()
        every { health } returns MutableStateFlow(BackupHealth.None)
    }

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

    private val today = MutableStateFlow(LocalDate(2026, 8, 28))

    private val observeSession = mockk<ObserveSessionUseCase>(relaxed = true) {
        every { this@mockk.invoke() } returns MutableSharedFlow<SessionStatus>(replay = 1)
    }

    private fun category(id: String, type: CategoryType) = Category(
        categoryId = CategoryId(id),
        name = id,
        icon = "star",
        color = "green",
        categoryType = type,
    )

    private fun buildViewModel() = ProfileViewModel(
        backupRepository = backupRepository,
        importData = mockk<ImportDataUseCase>(relaxed = true),
        signOut = mockk<SignOutUseCase>(relaxed = true),
        deleteUserAccount = mockk<DeleteUserAccountUseCase>(relaxed = true),
        backupController = backupController,
        backupVerifier = mockk<BackupVerifier>(relaxed = true),
        getBackupStaleness = mockk<GetBackupStalenessUseCase>(relaxed = true),
        logger = mockk<DiagnosticsLogger>(relaxed = true),
        localExportHistory = localExportHistory,
        todayFlow = FakeTodayFlow(today),
        categoryRepository = categoryRepository,
        getRecurringMonthlySummary = getRecurringMonthlySummary,
        observeSession = observeSession,
        appVersion = "1.0.0",
        clock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-08-28T15:04:05Z")
        },
    )

    @Test
    fun `the categories row splits the ledger's own categories by type`() = runTest(testDispatcher) {
        val categories = listOf(
            category("c1", CategoryType.Income),
            category("c2", CategoryType.Spend),
            category("c3", CategoryType.Spend),
        )
        every { categoryRepository.all() } returns flowOf(categories)

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(3, vm.state.value.categoryCount)
        assertEquals(1, vm.state.value.incomeCategoryCount)
    }

    @Test
    fun `the recurring row carries the summary the use case computed`() = runTest(testDispatcher) {
        every { getRecurringMonthlySummary.invoke() } returns
            flowOf(RecurringMonthlySummary(activeCount = 3, monthlyOutflow = Money(9_000L)))

        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(3, vm.state.value.recurringCount)
        assertEquals(Money(9_000L), vm.state.value.recurringMonthlyOutflow)
    }

    @Test
    fun `ExportSaved records the export, so the row stops saying Nunca`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(LastExportUi.Never, vm.state.value.lastExport)

        every { localExportHistory.daysSinceLastExport(any()) } returns 0
        vm.onIntent(ProfileIntent.ExportSaved)
        advanceUntilIdle()

        verify { localExportHistory.recordExport() }
        assertEquals(LastExportUi.DaysAgo(0), vm.state.value.lastExport)
    }

    @Test
    fun `requesting an export does not record one`() = runTest(testDispatcher) {
        coEvery { backupRepository.exportToJson(any(), any()) } returns "{}"

        val vm = buildViewModel()
        vm.onIntent(ProfileIntent.ExportRequested)
        advanceUntilIdle()

        verify(exactly = 0) { localExportHistory.recordExport() }
        assertEquals(LastExportUi.Never, vm.state.value.lastExport)
    }

    @Test
    fun `the export label ages across midnight with no intent in between`() = runTest(testDispatcher) {
        val exportDay = today.value
        val nextDay = exportDay.plus(1, DateTimeUnit.DAY)
        every { localExportHistory.daysSinceLastExport(exportDay) } returns 0
        every { localExportHistory.daysSinceLastExport(nextDay) } returns 1

        val vm = buildViewModel()
        advanceUntilIdle()
        assertEquals(LastExportUi.DaysAgo(0), vm.state.value.lastExport)

        today.value = nextDay
        advanceUntilIdle()

        assertEquals(LastExportUi.DaysAgo(1), vm.state.value.lastExport)
    }
}
