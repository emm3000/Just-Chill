package com.emm.justchill.feature.account

import com.emm.justchill.core.domain.account.AccountType
import com.emm.justchill.core.domain.account.CreateAccountUseCase
import com.emm.justchill.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("IgnoredReturnValue")
class AddAccountViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val createAccount: CreateAccountUseCase = mockk<CreateAccountUseCase>()

    private fun viewModel(): AddAccountViewModel = AddAccountViewModel(createAccount)

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
}
