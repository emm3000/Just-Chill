package com.emm.justchill.hh.seetransactions

import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.AccountId
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.TransactionId
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.SearchTransactionsUseCase
import com.emm.domain.transaction.TransactionFilter
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionTotals
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/** Midday, so nothing in these tests depends on where a day boundary falls. */
private val NOON = LocalTime(12, 0)

class SeeTransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    // Mid-month noon UTC: YearMonth.current(fixedClock) is August 2026 in every timezone.
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-08-15T12:00:00Z")
    }
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
    }
    private val searchTransactions = mockk<SearchTransactionsUseCase>()

    private fun buildViewModel(
        clock: Clock = fixedClock,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): SeeTransactionsViewModel {
        every { searchTransactions.invoke(any()) } returns flowOf(emptyList())
        return SeeTransactionsViewModel(searchTransactions, categoryRepository, transactionRepository, clock, zone)
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

    private fun category(
        id: String,
        type: CategoryType = CategoryType.Spend,
        name: String = "Categoria $id",
    ) = Category(
        categoryId = CategoryId(id),
        name = name,
        icon = "icon",
        color = "green",
        categoryType = type,
    )

    // ---- Month window ----

    @Test
    fun `initial month is the clock's current month and the list queries its exact bounds`() =
        runTest(testDispatcher) {
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
    fun `the initial month is read in the injected zone, not the device's`() = runTest(testDispatcher) {
        // One instant, two zones, two different months: 2026-09-01T02:00Z is already September at
        // UTC and still 31 August at UTC-5. Which month the list opens on is therefore a question
        // about the zone, not only about the clock — and the zone this ViewModel already takes for
        // its HOY/AYER headers has to be the one that answers it. While the month read the ambient
        // zone, varying the zone in a test moved the labels and left the window alone.
        //
        // Both zones are asserted because one proves nothing: on a machine sitting in that zone the
        // ambient read agrees. This one is America/Lima, i.e. exactly UTC-5.
        val nearMidnight = object : Clock {
            override fun now(): Instant = Instant.parse("2026-09-01T02:00:00Z")
        }
        val august = YearMonth(2026, Month.AUGUST)
        val september = YearMonth(2026, Month.SEPTEMBER)
        stubRange(august, flowOf(emptyList()))
        stubRange(september, flowOf(emptyList()))
        every { searchTransactions.invoke(any()) } returns flowOf(emptyList())

        val inLima = buildViewModel(nearMidnight, UtcOffset(hours = -5).asTimeZone())
        val inUtc = buildViewModel(nearMidnight, UtcOffset(hours = 0).asTimeZone())
        advanceUntilIdle()

        assertEquals(august, inLima.state.value.month)
        assertEquals(september, inUtc.state.value.month)
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

    // ---- Month mode vs global search mode ----

    @Test
    fun `an active filter switches the stream to global search and leaves the month window`() =
        runTest(testDispatcher) {
            val vm = buildViewModel()
            advanceUntilIdle()

            vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
            advanceTimeBy(300L)
            advanceUntilIdle()

            verify { searchTransactions.invoke(TransactionFilter(query = "café")) }
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

    // ---- Monthly summary ----

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

    // ---- A broken query must not kill the screen ----

    @Test
    fun `a failing month query degrades to an empty list and the next month still loads`() =
        runTest(testDispatcher) {
            val next = currentMonth.next()
            stubRange(currentMonth, flow { error("range query exploded") })
            stubRange(next, MutableStateFlow(listOf(tx("t-sep", TransactionType.Spend, 2_000, month = next))))
            totalsFlow.value = TransactionTotals(balance = Money(10_000), movementCount = 3)

            val vm = buildViewModel()
            advanceUntilIdle()

            assertTrue(vm.state.value.days.isEmpty())

            // The collector has to have survived the failure, or the arrows are dead for good.
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
            every { searchTransactions.invoke(any()) } returns flow { error("search exploded") }
            monthTransactionsFlow.value = listOf(tx("t-aug", TransactionType.Spend, 1_000))
            totalsFlow.value = TransactionTotals(balance = Money(10_000), movementCount = 3)

            val vm = SeeTransactionsViewModel(
                searchTransactions,
                categoryRepository,
                transactionRepository,
                fixedClock,
            )
            advanceUntilIdle()

            vm.onIntent(SeeTransactionsIntent.OnQueryChanged("café"))
            advanceTimeBy(300L)
            advanceUntilIdle()

            assertTrue(vm.state.value.days.isEmpty())

            vm.onIntent(SeeTransactionsIntent.OnClearFilters)
            advanceUntilIdle()

            assertEquals(listOf("t-aug"), vm.state.value.days.flatMap { d -> d.transactions.map { it.transactionId } })
        }

    // ---- "today" is a question about a zone ----

    @Test
    fun `the day header resolves HOY against the injected zone, not the machine's`() =
        runTest(testDispatcher) {
            // One instant, two answers. 2026-08-15 22:00 in Lima is already 2026-08-16 08:00 in
            // Karachi, so a movement on the 15th is HOY for one user and AYER for the other.
            //
            // Finding #7 in docs/DATE_AUDIT.md was that no test could ever assert this, because
            // the zone was read from the environment while the clock was injected. It is injected
            // now, and this is the assertion that was previously impossible to write.
            val eveningInLima = object : Clock {
                override fun now(): Instant = Instant.parse("2026-08-16T03:00:00Z")
            }
            monthTransactionsFlow.value = listOf(
                tx("t-1", TransactionType.Spend, 1_000, daysIntoMonth = 15),
            )

            val lima = SeeTransactionsViewModel(
                searchTransactions,
                categoryRepository,
                transactionRepository,
                eveningInLima,
                TimeZone.of("America/Lima"),
            )
            val karachi = SeeTransactionsViewModel(
                searchTransactions,
                categoryRepository,
                transactionRepository,
                eveningInLima,
                TimeZone.of("Asia/Karachi"),
            )
            advanceUntilIdle()

            assertEquals("HOY", lima.state.value.days.single().primaryLabel)
            assertEquals("AYER", karachi.state.value.days.single().primaryLabel)
        }

    // ---- Empty-state split ----

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

    // ---- Sheet ranking ----
    //
    // The filter sheet is now the only way into a category filter, and it lists 23 of them. Usage
    // order is what makes it fast to use, so the order is part of the contract, not a detail.

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

    // ---- Filter plumbing (behavior carried over from the pre-window list) ----

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
                match { it.categoryIds.contains(CategoryId("cat-1")) },
            )
        }
    }

    @Test
    fun `OnCategoryToggled twice with same id toggles off and returns to month mode`() =
        runTest(testDispatcher) {
            // The category must exist, or the deleted-category auto-reset clears the filter
            // between the two toggles and the second one re-activates instead of toggling off.
            categoriesFlow.value = listOf(category("cat-1"))
            val vm = buildViewModel()
            advanceUntilIdle()

            vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
            advanceUntilIdle()
            vm.onIntent(SeeTransactionsIntent.OnCategoryToggled("cat-1"))
            advanceUntilIdle()

            // An empty filter no longer searches: it re-subscribes the month window.
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
}
