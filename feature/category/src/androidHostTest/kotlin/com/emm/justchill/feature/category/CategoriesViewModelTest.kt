package com.emm.justchill.feature.category

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.category.DeleteCategoryUseCase
import com.emm.justchill.core.domain.category.UpdateCategoryUseCase
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class CategoriesViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val categoryRepository: CategoryRepository = mockk<CategoryRepository>()
    private val transactionRepository: TransactionRepository = mockk<TransactionRepository>()

    private val groceries: Category = category(id = "cat-1", name = "Mercado")

    private fun categoriesViewModel(): CategoriesViewModel {
        every { categoryRepository.all() } returns flowOf(listOf(groceries))
        every { transactionRepository.fetchAllWithCategory() } returns flowOf(emptyList())
        return CategoriesViewModel(
            categoryRepository,
            transactionRepository,
            UpdateCategoryUseCase(categoryRepository),
            DeleteCategoryUseCase(categoryRepository),
        )
    }

    @Test
    fun `a refused rename closes its dialog and says why once, as a refused delete does`() = runTest {
        val viewModel: CategoriesViewModel = categoriesViewModel()
        val dialogs: MutableList<Pair<Category?, String>> = mutableListOf()
        val effects: MutableList<CategoriesEffect> = mutableListOf()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.map { it.pendingEdit to it.editName }.distinctUntilChanged().collect { dialogs += it }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.collect { effects += it }
        }

        viewModel.onIntent(CategoriesIntent.OnEditClick(groceries))
        viewModel.onIntent(CategoriesIntent.OnEditNameChange("   "))
        viewModel.onIntent(CategoriesIntent.OnEditConfirm)
        advanceUntilIdle()

        assertEquals(listOf(null to "", groceries to "Mercado", groceries to "   ", null to ""), dialogs)
        assertEquals(
            listOf<CategoriesEffect>(CategoriesEffect.ShowMessage("El nombre no puede estar vacío")),
            effects,
        )
    }
}
