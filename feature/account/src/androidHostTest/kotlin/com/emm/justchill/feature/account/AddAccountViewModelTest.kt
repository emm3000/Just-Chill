package com.emm.justchill.feature.account

import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.domain.account.CreateAccountUseCase
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.shared.error.ValidationCode
import com.emm.justchill.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("IgnoredReturnValue")
class AddAccountViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val createAccount: CreateAccountUseCase = mockk<CreateAccountUseCase>()

    private fun viewModel(): AddAccountViewModel = AddAccountViewModel(createAccount)

    private class Recording(val states: List<AddAccountUiState>, val effects: List<AddAccountEffect>)

    private fun TestScope.record(viewModel: AddAccountViewModel, vararg intents: AddAccountIntent): Recording {
        val states: MutableList<AddAccountUiState> = mutableListOf()
        val effects: MutableList<AddAccountEffect> = mutableListOf()
        val stateJob: Job = launch { viewModel.state.collect { states.add(it) } }
        val effectJob: Job = launch { viewModel.effect.collect { effects.add(it) } }
        runCurrent()
        intents.forEach { intent ->
            viewModel.onIntent(intent)
            advanceUntilIdle()
        }
        stateJob.cancel()
        effectJob.cancel()
        return Recording(states, effects)
    }

    private fun refuseBlankNames() {
        coEvery { createAccount(any(), any()) } throws
            DomainException.ValidationError("Name cannot be empty", ValidationCode.NameRequired)
    }

    @Test
    fun `grid offers every account type with its palette label`() {
        val options: List<TypeGridOption> = typeGridOptions()

        assertEquals(AccountType.entries, options.map { it.type })
        assertEquals(AccountType.entries.map { it.toLabel() }, options.map { it.label })
    }

    @Test
    fun `every account type can be selected and submitted`() = runTest(testDispatcher) {
        coEvery { createAccount(any(), any()) } returns Unit

        AccountType.entries.forEach { type ->
            val viewModel: AddAccountViewModel = viewModel()

            viewModel.onIntent(AddAccountIntent.OnNameChange("Cuenta"))
            viewModel.onIntent(AddAccountIntent.OnTypeChange(type))
            viewModel.onIntent(AddAccountIntent.OnSave)
            advanceUntilIdle()

            coVerify { createAccount(name = "Cuenta", type = type) }
        }
    }

    @Test
    fun `a blank name lands under the name field with no effect`() = runTest(testDispatcher) {
        refuseBlankNames()
        val viewModel: AddAccountViewModel = viewModel()

        val recording: Recording = record(viewModel, AddAccountIntent.OnNameChange("   "), AddAccountIntent.OnSave)

        assertEquals(listOf(null, null, "El nombre no puede estar vacío"), recording.states.map { it.nameError })
        assertTrue(recording.effects.isEmpty())
    }

    @Test
    fun `typing in the name clears its error`() = runTest(testDispatcher) {
        refuseBlankNames()
        val viewModel: AddAccountViewModel = viewModel()
        record(viewModel, AddAccountIntent.OnNameChange("   "), AddAccountIntent.OnSave)

        val recording: Recording = record(viewModel, AddAccountIntent.OnNameChange("   a"))

        assertEquals(listOf("El nombre no puede estar vacío", null), recording.states.map { it.nameError })
    }

    @Test
    fun `a database failure stays a ShowError and leaves the name field clean`() = runTest(testDispatcher) {
        coEvery { createAccount(any(), any()) } throws DomainException.DatabaseError(RuntimeException("disk"))
        val viewModel: AddAccountViewModel = viewModel()

        val recording: Recording = record(viewModel, AddAccountIntent.OnNameChange("Cuenta"), AddAccountIntent.OnSave)

        assertEquals(
            listOf<AddAccountEffect>(AddAccountEffect.ShowError("Hubo un problema guardando tu data")),
            recording.effects,
        )
        assertEquals(listOf(null, null), recording.states.map { it.nameError })
    }
}
