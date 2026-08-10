package com.emm.justchill.hh.seetransactions

import androidx.lifecycle.viewModelScope
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.SearchTransactionsUseCase
import com.emm.domain.transaction.TransactionFilter
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionTotals
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

private const val SEARCH_DEBOUNCE_MS = 250L

class SeeTransactionsViewModel(
    private val searchTransactions: SearchTransactionsUseCase,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    private val clock: Clock = Clock.System,
) : MviViewModel<SeeTransactionsUiState, SeeTransactionsIntent, SeeTransactionsEffect>() {

    override val initialState = SeeTransactionsUiState(month = YearMonth.current(clock))

    private val filter = MutableStateFlow(TransactionFilter.None)
    private val selectedMonth = MutableStateFlow(YearMonth.current(clock))

    init {
        // Auto-reset filter if the active category got deleted out from under us.
        combine(categoryRepository.all(), filter) { categories, current ->
            val activeId = current.categoryIds.firstOrNull()
            if (activeId != null && categories.none { it.categoryId == activeId }) {
                filter.value = current.copy(categoryIds = emptySet())
            }
        }.launchIn(viewModelScope)

        // Sheet items + counts + the active category, ranked by the DB-side usage aggregate.
        combine(
            categoryRepository.all(),
            transactionRepository.observeCategoryUsageCounts(),
            filter,
        ) { categories, usageCounts, current ->
            buildCategoryFilterState(categories, usageCounts, current)
        }
            .onEach { categoryFilter ->
                updateState {
                    copy(
                        activeCategory = categoryFilter.activeCategory,
                        sheetItems = categoryFilter.sheetItems,
                        incomeCount = categoryFilter.incomeCount,
                        spendCount = categoryFilter.spendCount,
                    )
                }
            }
            .launchIn(viewModelScope)

        // App-level emptiness from one aggregate row — this is what tells the two empty
        // states apart ("nothing ever recorded" vs "nothing this month").
        transactionRepository.observeTotals()
            .map<TransactionTotals, Long?> { totals -> totals.movementCount }
            // A broken aggregate leaves the count unknown, never "known zero": one failed query
            // must not make the screen announce that the whole ledger is empty.
            .catch { emit(null) }
            .onEach { count -> updateState { copy(movementCount = count) } }
            .launchIn(viewModelScope)

        // The list: windowed to the selected month unless a filter is active — then it becomes
        // a global cross-month search, the deliberate escape hatch (capped in SQL).
        combine(selectedMonth, filter, ::Pair)
            .debounce { (_, currentFilter) -> if (currentFilter.query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
            .distinctUntilChanged()
            .flatMapLatest { (month, currentFilter) ->
                // Both branches catch their own failures. A `.catch` out here would terminate the
                // whole collector on the first database error and nothing would ever re-subscribe:
                // the month arrows would be dead until the ViewModel was recreated. Caught inside,
                // one broken query degrades to an empty slice and the next tap queries again.
                if (currentFilter.isEmpty) {
                    transactionRepository
                        .fetchAllWithCategoryInRange(month.startInclusiveMillis(), month.endExclusiveMillis())
                        .map { transactions ->
                            ListSlice(
                                month = month,
                                days = transactions.toDayGroups(today()),
                                summary = transactions.toMonthSummary(),
                            )
                        }
                        .catch { emit(ListSlice.emptyFor(month)) }
                } else {
                    searchTransactions(currentFilter)
                        .map { transactions ->
                            ListSlice(month = null, days = transactions.toDayGroups(today()), summary = null)
                        }
                        .catch { emit(ListSlice.EmptyResults) }
                }
            }
            .onEach { slice -> updateState { withListSlice(slice, selectedMonth.value) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SeeTransactionsIntent) {
        when (intent) {
            SeeTransactionsIntent.OnPreviousMonth -> selectMonth(selectedMonth.value.previous())

            SeeTransactionsIntent.OnNextMonth -> selectMonth(selectedMonth.value.next())

            is SeeTransactionsIntent.OnQueryChanged -> {
                filter.value = filter.value.copy(query = intent.query)
                updateState { copy(query = intent.query) }
            }

            is SeeTransactionsIntent.OnCategoryToggled -> {
                val id = CategoryId(intent.categoryId)
                val current = filter.value.categoryIds
                val next = if (id in current) emptySet() else setOf(id)
                filter.value = filter.value.copy(categoryIds = next)
            }

            is SeeTransactionsIntent.OnCategorySelected -> {
                val id = CategoryId(intent.categoryId)
                filter.value = filter.value.copy(categoryIds = setOf(id))
            }

            SeeTransactionsIntent.OnClearCategoryFilter -> {
                filter.value = filter.value.copy(categoryIds = emptySet())
            }

            SeeTransactionsIntent.OnClearFilters -> {
                filter.value = TransactionFilter.None
                updateState { copy(query = "") }
            }
        }
    }

    /**
     * Moves the window. The label is part of the tap, not part of the answer: it advances here,
     * synchronously, so the arrow gives feedback without waiting for a database round-trip. The
     * rows catch up when the new month's slice lands, and [withListSlice] drops any slice that
     * still answers for the month just left.
     */
    private fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
        updateState { copy(month = month) }
    }

    /**
     * The reference date the day headers resolve HOY/AYER against, read once per mapping pass so
     * every group in one emission agrees — and so the branch is testable through the injected clock.
     */
    private fun today(): LocalDate = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    /**
     * The sheet is the only entry point into a category filter, and it lists every category the
     * user owns — 23 of them on a stock install. Alphabetical made the common pick a scroll; the
     * `countPerCategory` aggregate already knows which ones actually get used, so it orders them.
     *
     * The sort is global but the sheet segments by type, and a stable ordering restricted to a
     * subset keeps that subset's relative order — so "most used first" holds inside Ingresos and
     * inside Gastos without sorting each half separately. Name breaks ties so the order is
     * deterministic: an unused category has no usage row at all, and two of them at count 0 must
     * not swap places between emissions.
     */
    private fun buildCategoryFilterState(
        categories: List<Category>,
        usageCounts: Map<CategoryId, Int>,
        current: TransactionFilter,
    ): CategoryFilterState {
        val activeId: CategoryId? = current.categoryIds.firstOrNull()

        val activeInfo = activeId?.let { id ->
            categories.firstOrNull { it.categoryId == id }?.let {
                ActiveCategoryInfo(it.categoryId.value, it.name)
            }
        }

        val sheetItems = categories
            .sortedWith(
                compareByDescending<Category> { usageCounts[it.categoryId] ?: 0 }
                    .thenBy { it.name.lowercase() },
            )
            .map { cat ->
                CategorySheetItem(
                    id = cat.categoryId.value,
                    name = cat.name,
                    iconId = cat.icon,
                    colorId = cat.color,
                    type = cat.categoryType,
                    isActive = cat.categoryId == activeId,
                )
            }

        val incomeCount = categories.count { it.categoryType == CategoryType.Income }
        val spendCount = categories.count { it.categoryType == CategoryType.Spend }

        return CategoryFilterState(activeInfo, sheetItems, incomeCount, spendCount)
    }

    private data class CategoryFilterState(
        val activeCategory: ActiveCategoryInfo?,
        val sheetItems: List<CategorySheetItem>,
        val incomeCount: Int,
        val spendCount: Int,
    )
}

/**
 * One emission of the list stream: the grouped days plus the summary (null in search mode).
 *
 * [month] is the month the slice answers for, or null when it came from a search — search results
 * are cross-month by design and belong to no single window.
 */
internal data class ListSlice(val month: YearMonth?, val days: List<DayGroup>, val summary: MonthSummaryUi?) {
    companion object {
        fun emptyFor(month: YearMonth) = ListSlice(month = month, days = emptyList(), summary = null)

        val EmptyResults = ListSlice(month = null, days = emptyList(), summary = null)
    }
}

/**
 * Applies a slice, unless it is a late answer for a month the user has already left — that one
 * would pair the new month's label with the old month's rows and totals. Search slices carry no
 * month and always apply.
 */
internal fun SeeTransactionsUiState.withListSlice(slice: ListSlice, selectedMonth: YearMonth): SeeTransactionsUiState {
    val isStale = slice.month != null && slice.month != selectedMonth
    return if (isStale) this else copy(days = slice.days, summary = slice.summary)
}

private fun List<TransactionWithCategory>.toDayGroups(today: LocalDate): List<DayGroup> = groupBy { transaction ->
    Instant.fromEpochMilliseconds(transaction.date)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
}.map { (date, transactions) -> DayGroup(date = date, today = today, transactions = transactions.toUi()) }

// Single pass over the month's raw amounts, before any toUi mapping. The window bounds the
// input to one month's rows, so this stays cheap on every emission.
private fun List<TransactionWithCategory>.toMonthSummary(): MonthSummaryUi {
    var incomeCents = 0L
    var spendCents = 0L
    forEach { transaction ->
        when (transaction.type) {
            TransactionType.Income -> incomeCents += transaction.amount.cents
            TransactionType.Spend -> spendCents += transaction.amount.cents
        }
    }
    return MonthSummaryUi(income = Money(incomeCents), spend = Money(spendCents))
}
