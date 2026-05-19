package com.emm.justchill.hh.profile

import com.emm.domain.shared.backup.ExportDataUseCase
import com.emm.domain.shared.backup.ImportDataUseCase
import com.emm.domain.shared.error.DomainException
import com.emm.justchill.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val exportData = mockk<ExportDataUseCase>()
    private val importData = mockk<ImportDataUseCase>(relaxed = true)

    private fun buildViewModel() = ProfileViewModel(exportData, importData)

    @Test
    fun `ExportToStream happy path writes json to stream and emits success message`() = runTest(testDispatcher) {
        val expectedJson = """{"version":"1.0","data":[]}"""
        coEvery { exportData(any(), any()) } returns expectedJson

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        val outputStream = ByteArrayOutputStream()
        vm.onIntent(ProfileIntent.ExportToStream(outputStream))
        advanceUntilIdle()

        assertEquals(expectedJson, outputStream.toString(Charsets.UTF_8.name()))
        assertTrue(effects.any { it is ProfileEffect.ShowMessage && it.text == "Listo, tu data está guardada." })
        assertEquals(false, vm.state.value.isExporting)

        job.cancel()
    }

    @Test
    fun `ExportToStream on DatabaseError emits Spanish error message`() = runTest(testDispatcher) {
        val cause = RuntimeException("db failure")
        coEvery { exportData(any(), any()) } throws DomainException.DatabaseError(cause)

        val vm = buildViewModel()
        val effects = mutableListOf<ProfileEffect>()
        val job = launch { vm.effect.collect { effects.add(it) } }

        val outputStream = ByteArrayOutputStream()
        vm.onIntent(ProfileIntent.ExportToStream(outputStream))
        advanceUntilIdle()

        assertTrue(effects.any { it is ProfileEffect.ShowMessage && it.text == "Hubo un problema guardando tu data" })
        assertEquals(false, vm.state.value.isExporting)

        job.cancel()
    }
}
