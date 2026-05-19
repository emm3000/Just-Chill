package com.emm.justchill.hh.profile

import com.emm.domain.shared.backup.ExportDataUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.backup.ImportStats
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelImportTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val exportData = mockk<ExportDataUseCase>(relaxed = true)
    private val importData = mockk<ImportDataUseCase>()

    private fun buildViewModel() = ProfileViewModel(exportData, importData)

    @Test
    fun `ImportJson happy path emits transaction count in Spanish success message`() = runTest(testDispatcher) {
        coEvery { importData(any()) } returns ImportStats(accounts = 2, categories = 5, transactions = 234)

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ImportJson("{}"))
        advanceUntilIdle()

        assertTrue(
            effects.any { it is ProfileEffect.ShowMessage && it.text == "Listo — 234 movimientos importados." },
            "Expected success message not found in $effects",
        )
        assertFalse(vm.state.value.isImporting)

        job.cancel()
    }

    @Test
    fun `ImportJson on ValidationError emits Spanish error from toUserMessage`() = runTest(testDispatcher) {
        coEvery { importData(any()) } throws DomainException.ValidationError("Archivo corrupto")

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        vm.onIntent(ProfileIntent.ImportJson("{ bad }"))
        advanceUntilIdle()

        // ValidationError.toUserMessage() returns the message directly
        assertTrue(
            effects.any { it is ProfileEffect.ShowMessage && it.text == "Archivo corrupto" },
            "Expected ValidationError message not found in $effects",
        )
        assertFalse(vm.state.value.isImporting)

        job.cancel()
    }

    @Test
    fun `ImportJson resets isImporting to false after completion`() = runTest(testDispatcher) {
        coEvery { importData(any()) } returns ImportStats(accounts = 1, categories = 1, transactions = 10)

        val vm = buildViewModel()

        vm.onIntent(ProfileIntent.ImportJson("{}"))
        advanceUntilIdle()

        assertEquals(false, vm.state.value.isImporting)
    }
}
