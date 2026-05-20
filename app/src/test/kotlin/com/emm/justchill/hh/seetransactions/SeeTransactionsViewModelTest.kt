package com.emm.justchill.hh.seetransactions

import com.emm.justchill.MainDispatcherRule
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.SearchTransactionsUseCase
import com.emm.domain.transaction.TransactionFilter
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionWithCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SeeTransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val categoriesFlow = MutableStateFlow(emptyList<com.emm.domain.category.Category>())
    private val allTransactionsFlow = MutableStateFlow(emptyList<TransactionWithCategory>())
    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns categoriesFlow
    }
    private val transactionRepository = mockk<TransactionRepository> {
        every { fetchAllWithCategory() } returns allTransactionsFlow
    }
    private val searchTransactions = mockk<SearchTransactionsUseCase>()

    private fun buildViewModel(): SeeTransactionsViewModel {
        every { searchTransactions.invoke(any()) } returns flowOf(emptyList())
        return SeeTransactionsViewModel(searchTransactions, categoryRepository, transactionRepository)
    }

    @Test
    fun `initial state has empty days, empty query, and filter not active`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.days.isEmpty())
        assertEquals("", state.query)
        assertFalse(state.isFilterActive)
        assertTrue(state.hasNoTransactionsAtAll)
        assertFalse(state.hasNoResultsForFilter)
    }

    @Test
    fun `OnQueryChanged updates query in state immediately`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))

        assertEquals("café", vm.state.value.query)
    }

    @Test
    fun `OnQueryChanged after debounce calls search use case`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        verify { searchTransactions.invoke(TransactionFilter(query = "café")) }
    }

    @Test
    fun `rapid OnQueryChanged calls only fire last query after debounce`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("c"))
        advanceTimeBy(100L)
        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("ca"))
        advanceTimeBy(100L)
        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("caf"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        verify(exactly = 0) { searchTransactions.invoke(TransactionFilter(query = "c")) }
        verify(exactly = 0) { searchTransactions.invoke(TransactionFilter(query = "ca")) }
        verify(atLeast = 1) { searchTransactions.invoke(TransactionFilter(query = "caf")) }
    }

    @Test
    fun `OnCategoryToggled adds category to filter immediately`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
        advanceUntilIdle()

        verify {
            searchTransactions.invoke(
                match { it.categoryIds.contains(CategoryId("cat-1")) }
            )
        }
    }

    @Test
    fun `OnCategoryToggled twice with same id toggles off`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
        advanceUntilIdle()
        vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
        advanceUntilIdle()

        verify {
            searchTransactions.invoke(
                match { it.categoryIds.isEmpty() }
            )
        }
    }

    @Test
    fun `OnClearFilters resets query and filter`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnClearFilters)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("", state.query)
        assertFalse(state.isFilterActive)
    }

    @Test
    fun `empty DB with no filter shows hasNoTransactionsAtAll`() = runTest(testDispatcher) {
        every { searchTransactions.invoke(any()) } returns flowOf(emptyList())
        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.hasNoTransactionsAtAll)
        assertFalse(state.hasNoResultsForFilter)
    }

    @Test
    fun `empty results with active filter shows hasNoResultsForFilter`() = runTest(testDispatcher) {
        every { searchTransactions.invoke(any()) } returns flowOf(emptyList())
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("nada"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.hasNoTransactionsAtAll)
        assertTrue(state.hasNoResultsForFilter)
    }
}
