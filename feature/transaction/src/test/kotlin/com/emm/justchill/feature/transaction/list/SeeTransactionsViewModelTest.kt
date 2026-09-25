package com.emm.justchill.feature.transaction.list

import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.justchill.core.domain.recurring.GetPendingRecurringMovementsUseCase
import com.emm.justchill.core.domain.recurring.PendingRecurring
import com.emm.justchill.core.domain.recurring.SkipRecurringMovementUseCase
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.TransactionId
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.transaction.TransactionFilter
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.domain.transaction.TransactionTotals
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.TransactionWithCategory
import com.emm.justchill.core.testing.FakeTodayFlow
import com.emm.justchill.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Midday, so nothing in these tests depends on where a day boundary falls.
private val NOON = LocalTime(12, 0)

// Mid-month, so nothing in these tests depends on where a month boundary falls.
private val TODAY = LocalDate(2026, 8, 15)

@Suppress("IgnoredReturnValue")
class SeeTransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val currentMonth = YearMonth(2026, Month.AUGUST)

    private val categoriesFlow = MutableStateFlow(emptyList<Category>())
    private val usageCountsFlow = MutableStateFlow(emptyMap<CategoryId, Int>())
    private val totalsFlow = MutableStateFlow(TransactionTotals.Empty)
    private val monthTransactionsFlow = MutableStateFlow(emptyList<TransactionWithCategory>())

    private val categoryRepository = mockk<CategoryRepository> {
        every { all() } returns categoriesFlow
    }
    private val transactionRepository = mockk<TransactionRepository> {
        every { observeCategoryUsageCounts() } returns usageCountsFlow
        every { observeTotals() } returns totalsFlow
        every { fetchAllWithCategoryInRange(any(), any()) } returns monthTransactionsFlow
        every { searchWithCategory(any()) } returns flowOf(emptyList())
    }
    private val pendingFlow = MutableStateFlow(emptyList<PendingRecurring>())
    private val getPendingRecurringMovements = mockk<GetPendingRecurringMovementsUseCase> {
        every { this@mockk(any()) } returns pendingFlow
    }
    private val confirmRecurring = mockk<ConfirmRecurringMovementUseCase>()
    private val skipRecurring = mockk<SkipRecurringMovementUseCase>()

    private fun buildViewModel(today: MutableStateFlow<LocalDate> = MutableStateFlow(TODAY)) = SeeTransactionsViewModel(
        categoryRepository,
        transactionRepository,
        getPendingRecurringMovements,
        confirmRecurring,
        skipRecurring,
        FakeTodayFlow(today),
    )

    @Test
    fun `today reaches the state even when the pending flow never emits`() = runTest(testDispatcher) {
        // The nudge card reads state.today. Sourcing it from the pending stream made a broken
        // recurring query silently hide the nudge instead of only the pending rows.
        every { getPendingRecurringMovements(any()) } returns emptyFlow()

        val viewModel = buildViewModel()
        advanceUntilIdle()

        assertEquals(TODAY, viewModel.state.value.today)
    }

    private fun tx(
        id: String,
        type: TransactionType,
        cents: Long,
        daysIntoMonth: Int = 5,
        month: YearMonth = currentMonth,
    ) = TransactionWithCategory(
        transactionId = TransactionId(id),
        type = type,
        amount = Money(cents),
        description = "movimiento $id",
        occurredAt = LocalDateTime(LocalDate(month.year, month.month, daysIntoMonth), NOON),
        accountId = AccountId("acc-1"),
        accountName = "BCP",
        category = null,
    )

    private fun stubRange(month: YearMonth, flow: Flow<List<TransactionWithCategory>>) {
        every {
            transactionRepository.fetchAllWithCategoryInRange(
                month.startInclusiveDay(),
                month.endExclusiveDay(),
            )
        } returns flow
    }

    private fun category(id: String, type: CategoryType = CategoryType.Spend, name: String = "Categoria $id") =
        Category(
            categoryId = CategoryId(id),
            name = name,
            icon = "icon",
            color = "green",
            categoryType = type,
        )

    @Test
    fun `initial month is today's month and the list queries its exact bounds`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(currentMonth, vm.state.value.month)
        verify {
            transactionRepository.fetchAllWithCategoryInRange(
                currentMonth.startInclusiveDay(),
                currentMonth.endExclusiveDay(),
            )
        }
    }

    @Test
    fun `the initial month is the month of the date todayFlow reports`() = runTest(testDispatcher) {
        // The initial month and its first range query come from todayFlow's date, not from a
        // clock the ViewModel reads for itself.
        val august = YearMonth(2026, Month.AUGUST)
        val september = YearMonth(2026, Month.SEPTEMBER)
        stubRange(august, flowOf(emptyList()))
        stubRange(september, flowOf(emptyList()))

        val onAugust31 = buildViewModel(MutableStateFlow(LocalDate(2026, 8, 31)))
        val onSeptember1 = buildViewModel(MutableStateFlow(LocalDate(2026, 9, 1)))
        advanceUntilIdle()

        assertEquals(august, onAugust31.state.value.month)
        assertEquals(september, onSeptember1.state.value.month)
        verify {
            transactionRepository.fetchAllWithCategoryInRange(august.startInclusiveDay(), august.endExclusiveDay())
            transactionRepository.fetchAllWithCategoryInRange(
                september.startInclusiveDay(),
                september.endExclusiveDay(),
            )
        }
    }

    @Test
    fun `OnNextMonth requeries with the next month's bounds`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnNextMonth)
        advanceUntilIdle()

        val next = currentMonth.next()
        assertEquals(next, vm.state.value.month)
        verify {
            transactionRepository.fetchAllWithCategoryInRange(
                next.startInclusiveDay(),
                next.endExclusiveDay(),
            )
        }
    }

    @Test
    fun `OnNextMonth moves the label without waiting for the database`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnNextMonth)

        // No advanceUntilIdle on purpose: nothing has been collected and no query has answered,
        // yet the arrow the user tapped must already be reflected in the selector's label.
        assertEquals(currentMonth.next(), vm.state.value.month)
    }

    @Test
    fun `OnPreviousMonth moves the label without waiting for the database`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnPreviousMonth)

        assertEquals(currentMonth.previous(), vm.state.value.month)
    }

    @Test
    fun `the rows on screen belong to the month the label names`() = runTest(testDispatcher) {
        val next = currentMonth.next()
        stubRange(currentMonth, MutableStateFlow(listOf(tx("t-aug", TransactionType.Spend, 1_000))))
        stubRange(next, MutableStateFlow(listOf(tx("t-sep", TransactionType.Spend, 2_000, month = next))))

        val vm = buildViewModel()
        advanceUntilIdle()
        vm.onIntent(SeeTransactionsIntent.OnNextMonth)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(next, state.month)
        assertEquals(listOf("t-sep"), state.days.flatMap { day -> day.transactions.map { it.transactionId } })
    }

    @Test
    fun `OnPreviousMonth requeries with the previous month's bounds`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnPreviousMonth)
        advanceUntilIdle()

        val previous = currentMonth.previous()
        assertEquals(previous, vm.state.value.month)
        verify {
            transactionRepository.fetchAllWithCategoryInRange(
                previous.startInclusiveDay(),
                previous.endExclusiveDay(),
            )
        }
    }

    @Test
    fun `the browsed month follows a midnight rollover when the user never moved it`() = runTest(testDispatcher) {
        val today = MutableStateFlow(LocalDate(2026, 8, 31))
        val vm = buildViewModel(today)
        advanceUntilIdle()
        assertEquals(currentMonth, vm.state.value.month, "August IS the month on 31 August")

        today.value = LocalDate(2026, 9, 1)
        advanceUntilIdle()

        assertEquals(currentMonth.next(), vm.state.value.month, "the browsed month must follow the calendar")
    }

    @Test
    fun `the browsed month does not follow a midnight rollover once the user arrowed away`() = runTest(testDispatcher) {
        val today = MutableStateFlow(LocalDate(2026, 8, 31))
        val vm = buildViewModel(today)
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnPreviousMonth)
        advanceUntilIdle()
        assertEquals(currentMonth.previous(), vm.state.value.month)

        today.value = LocalDate(2026, 9, 1)
        advanceUntilIdle()

        assertEquals(currentMonth.previous(), vm.state.value.month, "a deliberately browsed month must not move")
    }

    @Test
    fun `an active filter switches the stream to global search and leaves the month window`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()
            advanceUntilIdle()

            vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
            advanceTimeBy(300L)
            advanceUntilIdle()

            verify { transactionRepository.searchWithCategory(TransactionFilter(query = "café")) }
            // flatMapLatest switched away: the range query ran only for the initial subscription.
            verify(exactly = 1) { transactionRepository.fetchAllWithCategoryInRange(any(), any()) }
        }

    @Test
    fun `clearing filters returns to month mode and requeries the range`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnClearFilters)
        advanceUntilIdle()

        verify(exactly = 2) { transactionRepository.fetchAllWithCategoryInRange(any(), any()) }
    }

    @Test
    fun `month rows produce income, spend, and net totals in the summary`() = runTest(testDispatcher) {
        monthTransactionsFlow.value = listOf(
            tx("t-1", TransactionType.Income, cents = 10_000),
            tx("t-2", TransactionType.Spend, cents = 3_000),
            tx("t-3", TransactionType.Income, cents = 500),
        )
        val vm = buildViewModel()
        advanceUntilIdle()

        val summary = vm.state.value.summary
        assertEquals(Money(10_500), summary?.income)
        assertEquals(Money(3_000), summary?.spend)
        assertEquals(Money(7_500), summary?.net)
    }

    @Test
    fun `search mode has no summary`() = runTest(testDispatcher) {
        monthTransactionsFlow.value = listOf(tx("t-1", TransactionType.Income, cents = 10_000))
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        assertNull(vm.state.value.summary)
    }

    @Test
    fun `a failing month query degrades to an empty list and the next month still loads`() = runTest(testDispatcher) {
        val next = currentMonth.next()
        stubRange(currentMonth, flow { error("range query exploded") })
        stubRange(next, MutableStateFlow(listOf(tx("t-sep", TransactionType.Spend, 2_000, month = next))))
        totalsFlow.value = TransactionTotals(balance = Money(10_000), movementCount = 3)

        val vm = buildViewModel()
        advanceUntilIdle()

        assertTrue(vm.state.value.days.isEmpty())

        vm.onIntent(SeeTransactionsIntent.OnNextMonth)
        advanceUntilIdle()

        verify {
            transactionRepository.fetchAllWithCategoryInRange(
                next.startInclusiveDay(),
                next.endExclusiveDay(),
            )
        }
        assertEquals(listOf("t-sep"), vm.state.value.days.flatMap { d -> d.transactions.map { it.transactionId } })
    }

    @Test
    fun `a failing search degrades to an empty list and clearing the filter still loads the month`() =
        runTest(testDispatcher) {
            every { transactionRepository.searchWithCategory(any()) } returns flow { error("search exploded") }
            monthTransactionsFlow.value = listOf(tx("t-aug", TransactionType.Spend, 1_000))
            totalsFlow.value = TransactionTotals(balance = Money(10_000), movementCount = 3)

            val vm = buildViewModel()
            advanceUntilIdle()

            vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
            advanceTimeBy(300L)
            advanceUntilIdle()

            assertTrue(vm.state.value.days.isEmpty())

            vm.onIntent(SeeTransactionsIntent.OnClearFilters)
            advanceUntilIdle()

            assertEquals(listOf("t-aug"), vm.state.value.days.flatMap { d -> d.transactions.map { it.transactionId } })
        }

    @Test
    fun `the day header labels a row HOY or AYER against the date todayFlow reports`() = runTest(testDispatcher) {
        monthTransactionsFlow.value = listOf(
            tx("t-1", TransactionType.Spend, 1_000, daysIntoMonth = 15),
        )

        val onTheDay = buildViewModel(MutableStateFlow(LocalDate(2026, 8, 15)))
        val theDayAfter = buildViewModel(MutableStateFlow(LocalDate(2026, 8, 16)))
        advanceUntilIdle()

        assertEquals("HOY", onTheDay.state.value.days.single().primaryLabel)
        assertEquals("AYER", theDayAfter.state.value.days.single().primaryLabel)
    }

    @Test
    fun `HOY relabels to AYER across a midnight rollover with no user interaction`() = runTest(testDispatcher) {
        monthTransactionsFlow.value = listOf(tx("t-1", TransactionType.Spend, 1_000, daysIntoMonth = 15))
        val today = MutableStateFlow(LocalDate(2026, 8, 15))
        val vm = buildViewModel(today)
        advanceUntilIdle()
        assertEquals("HOY", vm.state.value.days.single().primaryLabel)

        today.value = LocalDate(2026, 8, 16)
        advanceUntilIdle()

        assertEquals("AYER", vm.state.value.days.single().primaryLabel)
    }

    @Test
    fun `before any emission the screen claims nothing and still offers the month selector`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()

            // Deliberately not advanced: this is the very first composition.
            val state = vm.state.value
            assertEquals(ListDisplayState.Loading, state.listDisplayState)
            assertTrue(state.isMonthSelectorVisible)
        }

    @Test
    fun `a failing totals aggregate leaves the count unknown instead of claiming an empty ledger`() =
        runTest(testDispatcher) {
            every { transactionRepository.observeTotals() } returns flow { error("totals exploded") }
            val vm = buildViewModel()
            advanceUntilIdle()

            val state = vm.state.value
            assertNull(state.movementCount)
            assertFalse(state.listDisplayState == ListDisplayState.EmptyLedger)
        }

    @Test
    fun `empty DB with no filter shows the empty ledger`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(ListDisplayState.EmptyLedger, vm.state.value.listDisplayState)
    }

    @Test
    fun `an empty month with movements elsewhere shows the empty month, not the empty ledger`() =
        runTest(testDispatcher) {
            totalsFlow.value = TransactionTotals(balance = Money(10_000), movementCount = 3)
            val vm = buildViewModel()
            advanceUntilIdle()

            assertEquals(ListDisplayState.EmptyMonth, vm.state.value.listDisplayState)
        }

    @Test
    fun `empty results with active filter shows no search results`() = runTest(testDispatcher) {
        totalsFlow.value = TransactionTotals(balance = Money(10_000), movementCount = 3)
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("nada"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        assertEquals(ListDisplayState.NoSearchResults, vm.state.value.listDisplayState)
    }

    @Test
    fun `searching a ledger that holds nothing stays on the empty ledger`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("nada"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        assertEquals(ListDisplayState.EmptyLedger, vm.state.value.listDisplayState)
    }

    @Test
    fun `sheet items rank by the usage counts map, not by folding transactions`() = runTest(testDispatcher) {
        // Names are alphabetical in id order here, so an alphabetical sort would answer a, b, c —
        // only a usage-ranked sort answers b, c, a.
        categoriesFlow.value = listOf(category("cat-a"), category("cat-b"), category("cat-c"))
        usageCountsFlow.value = mapOf(CategoryId("cat-b") to 5, CategoryId("cat-c") to 2)
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(listOf("cat-b", "cat-c", "cat-a"), vm.state.value.sheetItems.map { it.id })
        verify(exactly = 0) { transactionRepository.fetchAllWithCategory() }
    }

    @Test
    fun `categories tied on usage fall back to their name, ascending`() = runTest(testDispatcher) {
        // Emission order is Zapatos, Almuerzo, Mercado: a sort on usage alone would leave the tied
        // pair in that order. Only the name tiebreak puts Almuerzo before Zapatos.
        categoriesFlow.value = listOf(
            category("cat-1", name = "Zapatos"),
            category("cat-2", name = "Almuerzo"),
            category("cat-3", name = "Mercado"),
        )
        usageCountsFlow.value = mapOf(
            CategoryId("cat-1") to 4,
            CategoryId("cat-2") to 4,
            CategoryId("cat-3") to 9,
        )
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(listOf("Mercado", "Almuerzo", "Zapatos"), vm.state.value.sheetItems.map { it.name })
    }

    @Test
    fun `a category nobody has used yet is still offered in the sheet`() = runTest(testDispatcher) {
        // Ranking must reorder the list, never shorten it: a brand-new category has no usage row
        // at all, and dropping it would make it unreachable from the only filter entry point.
        categoriesFlow.value = listOf(category("used"), category("never-used"))
        usageCountsFlow.value = mapOf(CategoryId("used") to 3)
        val vm = buildViewModel()
        advanceUntilIdle()

        val sheetItems = vm.state.value.sheetItems
        assertEquals(2, sheetItems.size)
        assertTrue(sheetItems.any { it.id == "never-used" })
    }

    @Test
    fun `initial state has empty days, empty query, and filter not active`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.days.isEmpty())
        assertEquals("", state.query)
        assertFalse(state.isFilterActive)
        assertEquals(ListDisplayState.EmptyLedger, state.listDisplayState)
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

        verify { transactionRepository.searchWithCategory(TransactionFilter(query = "café")) }
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

        verify(exactly = 0) { transactionRepository.searchWithCategory(TransactionFilter(query = "c")) }
        verify(exactly = 0) { transactionRepository.searchWithCategory(TransactionFilter(query = "ca")) }
        verify(atLeast = 1) { transactionRepository.searchWithCategory(TransactionFilter(query = "caf")) }
    }

    @Test
    fun `OnCategoryToggled adds category to filter immediately`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
        advanceUntilIdle()

        verify {
            transactionRepository.searchWithCategory(
                match { it.categoryIds.contains(CategoryId("cat-1")) },
            )
        }
    }

    @Test
    fun `OnCategoryToggled twice with same id toggles off and returns to month mode`() = runTest(testDispatcher) {
        // The category must exist, or the deleted-category auto-reset clears the filter
        // between the two toggles and the second one re-activates instead of toggling off.
        categoriesFlow.value = listOf(category("cat-1"))
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
        advanceUntilIdle()
        vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
        advanceUntilIdle()

        // An empty filter re-subscribes the month window instead of searching — that's the second call.
        verify(exactly = 2) { transactionRepository.fetchAllWithCategoryInRange(any(), any()) }
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
    fun `OnFilterSheetRequested opens the filter sheet and OnFilterSheetDismissed closes it`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()
            advanceUntilIdle()
            assertFalse(vm.state.value.showFilterSheet)

            vm.onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnFilterSheetRequested)
            advanceUntilIdle()
            assertTrue(vm.state.value.showFilterSheet)

            vm.onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnFilterSheetDismissed)
            advanceUntilIdle()
            assertFalse(vm.state.value.showFilterSheet)
        }

    @Test
    fun `OnSearchRequested opens search and OnSearchClosed closes it again`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()
        assertFalse(vm.state.value.isSearchOpen)

        vm.onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnSearchRequested)
        advanceUntilIdle()
        assertTrue(vm.state.value.isSearchOpen)

        vm.onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnSearchClosed)
        advanceUntilIdle()
        assertFalse(vm.state.value.isSearchOpen)
    }

    @Test
    fun `a confirmed minimum bound reaches searchWithCategory in the filter`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val states = mutableListOf<Money?>()
        backgroundScope.launch { vm.state.collect { states += it.minAmount } }
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetRequested(AmountRangeTarget.Min))
        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountConfirmed("2000"))
        advanceUntilIdle()

        assertEquals(listOf(null, Money(2_000L)), states)
        verify { transactionRepository.searchWithCategory(match { it.minAmount == Money(2_000L) }) }
    }

    @Test
    fun `a confirmed maximum bound reaches searchWithCategory in the filter`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val states = mutableListOf<Money?>()
        backgroundScope.launch { vm.state.collect { states += it.maxAmount } }
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetRequested(AmountRangeTarget.Max))
        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountConfirmed("5000"))
        advanceUntilIdle()

        assertEquals(listOf(null, Money(5_000L)), states)
        verify { transactionRepository.searchWithCategory(match { it.maxAmount == Money(5_000L) }) }
    }

    @Test
    fun `a minimum set above an existing maximum arrives swapped`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val ranges = mutableListOf<Pair<Money?, Money?>>()
        backgroundScope.launch { vm.state.collect { ranges += it.minAmount to it.maxAmount } }
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetRequested(AmountRangeTarget.Max))
        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountConfirmed("2000"))
        advanceUntilIdle()
        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetRequested(AmountRangeTarget.Min))
        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountConfirmed("5000"))
        advanceUntilIdle()

        assertEquals(
            listOf(null to null, null to Money(2_000L), Money(2_000L) to Money(5_000L)),
            ranges,
        )
        verify {
            transactionRepository.searchWithCategory(
                match { it.minAmount == Money(2_000L) && it.maxAmount == Money(5_000L) },
            )
        }
    }

    @Test
    fun `the banner clear resets category and range and keeps the text query`() = runTest(testDispatcher) {
        categoriesFlow.value = listOf(category("cat-1"))
        val vm = buildViewModel()
        val states = mutableListOf<SeeTransactionsUiState>()
        backgroundScope.launch { vm.state.collect { states += it } }
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
        vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetRequested(AmountRangeTarget.Min))
        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountConfirmed("2000"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnClearCategoryFilter)
        advanceUntilIdle()

        val state = states.last()
        assertEquals("café", state.query)
        assertEquals(null, state.activeCategory)
        assertEquals(null, state.minAmount)
        assertEquals(null, state.maxAmount)
    }

    @Test
    fun `OnClearFilters resets query, category and range together`() = runTest(testDispatcher) {
        val vm = buildViewModel()
        val states = mutableListOf<SeeTransactionsUiState>()
        backgroundScope.launch { vm.state.collect { states += it } }
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetRequested(AmountRangeTarget.Min))
        vm.onIntent(SeeTransactionsIntent.AmountFilterIntent.OnAmountConfirmed("2000"))
        advanceTimeBy(300L)
        advanceUntilIdle()

        vm.onIntent(SeeTransactionsIntent.OnClearFilters)
        advanceUntilIdle()

        val state = states.last()
        assertEquals("", state.query)
        assertEquals(null, state.minAmount)
        assertEquals(null, state.maxAmount)
        assertFalse(state.isFilterActive)
    }

    @Test
    fun `OnSearchClosed clears the query as well as searchRequested, and requeries the month range`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()
            advanceUntilIdle()

            vm.onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnSearchRequested)
            vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
            advanceTimeBy(300L)
            advanceUntilIdle()
            assertEquals("café", vm.state.value.query)

            vm.onIntent(SeeTransactionsIntent.ScreenChromeIntent.OnSearchClosed)
            advanceUntilIdle()

            assertEquals("", vm.state.value.query)
            assertFalse(vm.state.value.isSearchOpen)
            verify(exactly = 2) { transactionRepository.fetchAllWithCategoryInRange(any(), any()) }
        }
}
