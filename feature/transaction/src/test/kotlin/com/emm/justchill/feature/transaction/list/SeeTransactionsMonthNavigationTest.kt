package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.domain.transaction.TransactionTotals
import com.emm.justchill.core.testing.FakeTodayFlow
import com.emm.justchill.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SeeTransactionsMonthNavigationTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val september: YearMonth = YearMonth(2026, Month.SEPTEMBER)
    private val august: YearMonth = YearMonth(2026, Month.AUGUST)
    private val march: YearMonth = YearMonth(2026, Month.MARCH)
    private val todayDates: MutableStateFlow<LocalDate> = MutableStateFlow(LocalDate(2026, Month.SEPTEMBER, 26))

    private val categoryRepository: CategoryRepository = mockk {
        every { all() } returns flowOf(emptyList())
    }
    private val transactionRepository: TransactionRepository = mockk {
        every { observeCategoryUsageCounts() } returns flowOf(emptyMap())
        every { observeTotals() } returns flowOf(TransactionTotals.Empty)
        every { fetchAllWithCategoryInRange(any(), any()) } returns flowOf(emptyList())
        every { searchWithCategory(any()) } returns flowOf(emptyList())
    }

    private fun buildViewModel(): SeeTransactionsViewModel = SeeTransactionsViewModel(
        categoryRepository,
        transactionRepository,
        FakeTodayFlow(todayDates),
    )

    @Test
    fun `a save dated today while browsing March lands on the current month`() = runTest(testDispatcher) {
        val vm: SeeTransactionsViewModel = buildViewModel()
        val months: MutableList<YearMonth> = mutableListOf()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.map { it.month }.distinctUntilChanged().collect { months += it }
        }
        advanceUntilIdle()
        vm.onIntent(SeeTransactionsIntent.OnMonthSelected(march))
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnMonthSelected(september))
        advanceUntilIdle()

        assertEquals(listOf(september, march, september), months)
        verify(exactly = 2) {
            transactionRepository.fetchAllWithCategoryInRange(
                september.startInclusiveDay(),
                september.endExclusiveDay(),
            )
        }
    }

    @Test
    fun `a save dated last month while browsing the current month lands on last month`() = runTest(testDispatcher) {
        val vm: SeeTransactionsViewModel = buildViewModel()
        val months: MutableList<YearMonth> = mutableListOf()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.map { it.month }.distinctUntilChanged().collect { months += it }
        }
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnMonthSelected(august))
        advanceUntilIdle()

        assertEquals(listOf(september, august), months)
        verify {
            transactionRepository.fetchAllWithCategoryInRange(
                august.startInclusiveDay(),
                august.endExclusiveDay(),
            )
        }
    }

    @Test
    fun `a save that lands on last month stops the list from following the rollover`() = runTest(testDispatcher) {
        val vm: SeeTransactionsViewModel = buildViewModel()
        val months: MutableList<YearMonth> = mutableListOf()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.map { it.month }.distinctUntilChanged().collect { months += it }
        }
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnMonthSelected(august))
        advanceUntilIdle()
        todayDates.value = LocalDate(2026, Month.OCTOBER, 1)
        advanceUntilIdle()

        assertEquals(listOf(september, august), months)
    }
}
