package com.emm.justchill.hh.seetransactions

import androidx.lifecycle.viewModelScope
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.recurring.ConfirmRecurringMovementUseCase
import com.emm.domain.recurring.GetPendingRecurringMovementsUseCase
import com.emm.domain.recurring.PendingRecurring
import com.emm.domain.recurring.SkipRecurringMovementUseCase
import com.emm.domain.shared.CategoryId
import com.emm.domain.shared.Money
import com.emm.domain.shared.RecurringMovementId
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionFilter
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionTotals
import com.emm.domain.transaction.TransactionType
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.time.TodayFlow
import com.emm.justchill.hh.recurring.toPendingRecurringUi
import com.emm.justchill.hh.transaction.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

private const val SEARCH_DEBOUNCE_MS = 250L

class SeeTransactionsViewModel(
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    private val getPendingRecurringMovements: GetPendingRecurringMovementsUseCase,
    private val confirmRecurringMovement: ConfirmRecurringMovementUseCase,
    private val skipRecurringMovement: SkipRecurringMovementUseCase,
    todayFlow: TodayFlow,
) : MviViewModel<SeeTransactionsUiState, SeeTransactionsIntent, SeeTransactionsEffect>() {

    // The screen's only derivation of "what day is it" — which is why no Clock reaches this class.
    // A second one would let the pending list and the HOY/AYER headers disagree about the date.
    private val today: StateFlow<LocalDate> = todayFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, todayFlow.today())

    override val initialState = SeeTransactionsUiState(month = YearMonth.of(today.value))

    private val filter = MutableStateFlow(TransactionFilter.None)
    private val selectedMonth = MutableStateFlow(initialState.month)

    init {
        combine(categoryRepository.all(), filter) { categories, current ->
            val activeId = current.categoryIds.firstOrNull()
            if (activeId != null && categories.none { it.categoryId == activeId }) {
                filter.value = current.copy(categoryIds = emptySet())
            }
        }.launchIn(viewModelScope)

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

        transactionRepository.observeTotals()
            .map<TransactionTotals, Long?> { totals -> totals.movementCount }
            .catch { emit(null) }
            .onEach { count -> updateState { copy(movementCount = count) } }
            .launchIn(viewModelScope)

        combine(selectedMonth, filter, ::Pair)
            .debounce { (_, currentFilter) -> if (currentFilter.query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
            .distinctUntilChanged()
            .flatMapLatest { (month, currentFilter) ->
                // Both branches catch their own failures: a `.catch` out here would kill the whole
                // collector on the first database error, leaving the month arrows dead until the
                // ViewModel is recreated. Caught inside, a broken query degrades to an empty slice.
                // Combined with the shared `today` so a midnight rollover re-labels HOY/AYER without
                // re-running the query underneath the rows.
                if (currentFilter.isEmpty) {
                    combine(
                        transactionRepository
                            .fetchAllWithCategoryInRange(month.startInclusiveDay(), month.endExclusiveDay()),
                        today,
                    ) { transactions, todayDate ->
                        ListSlice(
                            month = month,
                            days = transactions.toDayGroups(todayDate),
                            summary = transactions.toMonthSummary(),
                        )
                    }.catch { emit(ListSlice.emptyFor(month)) }
                } else {
                    combine(
                        transactionRepository.searchWithCategory(currentFilter),
                        today,
                    ) { transactions, todayDate ->
                        ListSlice(month = null, days = transactions.toDayGroups(todayDate), summary = null)
                    }.catch { emit(ListSlice.EmptyResults) }
                }
            }
            .onEach { slice -> updateState { withListSlice(slice, selectedMonth.value) } }
            .launchIn(viewModelScope)

        // A separate flow on purpose: pending recurring movements never depend on the browsed month
        // or the active filter. Driven by the shared `today` instead, so a movement that comes due
        // at midnight appears with no user interaction.
        today
            .flatMapLatest { date -> getPendingRecurringMovements(date).map { pending -> date to pending } }
            .onEach { (date, pending) -> updateState { mapToPendingUiState(pending, date) } }
            .launchIn(viewModelScope)

        // The browsed month tracks the calendar only while the user never moved it away. `scan`
        // carries the previous calendar month alongside the current one so the check needs no
        // mutable field: it advances exactly when it still equals the PREVIOUS tick's calendar
        // month. A user who arrowed away fails that check and keeps their month; a user who never
        // moved (or arrowed back) rolls over with the calendar.
        today
            .map { date -> YearMonth.of(date) }
            .distinctUntilChanged()
            .scan(CalendarMonthTick(previous = null, current = null)) { tick, month ->
                CalendarMonthTick(previous = tick.current, current = month)
            }
            .onEach { tick ->
                val previous = tick.previous
                val current = tick.current
                if (previous != null && current != null && selectedMonth.value == previous) {
                    selectMonth(current)
                }
            }
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

            is SeeTransactionsIntent.ConfirmRecurring -> onConfirmRecurring(intent)

            is SeeTransactionsIntent.SkipRecurring -> onSkipRecurring(intent)
        }
    }

    private fun onConfirmRecurring(intent: SeeTransactionsIntent.ConfirmRecurring) {
        launchSafe(onError = { e -> SeeTransactionsEffect.ShowError(e.toUserMessage()) }) {
            confirmRecurringMovement(
                templateId = RecurringMovementId(intent.templateId),
                yearMonth = intent.period,
                callerAmount = intent.callerAmount,
            )
            sendEffect(SeeTransactionsEffect.CloseConfirmSheet)
        }
    }

    private fun onSkipRecurring(intent: SeeTransactionsIntent.SkipRecurring) {
        launchSafe(onError = { e -> SeeTransactionsEffect.ShowError(e.toUserMessage()) }) {
            skipRecurringMovement(
                templateId = RecurringMovementId(intent.templateId),
                yearMonth = intent.period,
            )
            sendEffect(SeeTransactionsEffect.CloseConfirmSheet)
        }
    }

    /**
     * Advances the month synchronously so the tap gets instant feedback; the list stream lands the
     * actual rows later.
     */
    private fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
        updateState { copy(month = month) }
    }

    /**
     * The month comes from the date that produced [pending], not from the selected one: browsing to
     * March must not relabel March's own pending row, and it must not decide whether the section is
     * visible either.
     */
    private fun SeeTransactionsUiState.mapToPendingUiState(
        pending: List<PendingRecurring>,
        today: LocalDate,
    ): SeeTransactionsUiState {
        val currentMonth = YearMonth.of(today)
        return copy(
            pendingRecurringMovements = pending.map { it.toPendingRecurringUi(currentMonth) },
            currentMonth = currentMonth,
        )
    }

    /** Name breaks ties so two categories tied at zero usage don't swap places between emissions. */
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

internal data class ListSlice(val month: YearMonth?, val days: List<DayGroup>, val summary: MonthSummaryUi?) {
    companion object {
        fun emptyFor(month: YearMonth) = ListSlice(month = month, days = emptyList(), summary = null)

        val EmptyResults = ListSlice(month = null, days = emptyList(), summary = null)
    }
}

/** One `today` tick paired with the calendar month before it, so a rollover check needs no mutable field. */
private data class CalendarMonthTick(val previous: YearMonth?, val current: YearMonth?)

/**
 * Applies a slice, unless it is a late answer for a month the user has already left — that one
 * would pair the new month's label with the old month's rows and totals. Search slices carry no
 * month and always apply.
 */
internal fun SeeTransactionsUiState.withListSlice(slice: ListSlice, selectedMonth: YearMonth): SeeTransactionsUiState {
    val isStale = slice.month != null && slice.month != selectedMonth
    return if (isStale) this else copy(days = slice.days, summary = slice.summary)
}

/**
 * The day a row belongs under is the day it carries. No zone, no conversion, nothing that can put
 * the same transaction under a different header on a different device.
 */
private fun List<TransactionWithCategory>.toDayGroups(today: LocalDate): List<DayGroup> =
    groupBy { transaction -> transaction.occurredAt.date }
        .map { (date, transactions) ->
            DayGroup(date = date, today = today, transactions = transactions.toUi(today))
        }

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
