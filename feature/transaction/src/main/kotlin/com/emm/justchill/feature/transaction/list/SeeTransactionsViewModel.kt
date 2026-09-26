package com.emm.justchill.feature.transaction.list

import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.domain.category.Category
import com.emm.justchill.core.domain.category.CategoryRepository
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.CategoryId
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.domain.shared.YearMonth
import com.emm.justchill.core.domain.shared.error.DomainException
import com.emm.justchill.core.domain.time.TodayFlow
import com.emm.justchill.core.domain.transaction.TransactionFilter
import com.emm.justchill.core.domain.transaction.TransactionRepository
import com.emm.justchill.core.domain.transaction.TransactionTotals
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.domain.transaction.TransactionWithCategory
import com.emm.justchill.core.domain.transaction.withAmountRange
import com.emm.justchill.core.ui.error.toUserMessage
import com.emm.justchill.core.ui.format.centsToMoney
import com.emm.justchill.core.ui.mvi.MviViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

private const val SEARCH_DEBOUNCE_MS = 250L

class SeeTransactionsViewModel(
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
    todayFlow: TodayFlow,
) : MviViewModel<SeeTransactionsUiState, SeeTransactionsIntent, SeeTransactionsEffect>(
    // Same seed `today`'s stateIn below uses, read before that StateFlow's first collection — so
    // it always equals today.value here.
    SeeTransactionsUiState(month = YearMonth.of(todayFlow.today())),
) {

    // The screen's only derivation of "what day is it" — which is why no Clock reaches this class.
    // A second one would let the month selector and the HOY/AYER headers disagree about the date.
    private val today: StateFlow<LocalDate> = todayFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, todayFlow.today())

    private val filter = MutableStateFlow(TransactionFilter.None)
    private val selectedMonth = MutableStateFlow(initialState.month)
    private var calendarMonth = initialState.month
    private val onDomainError: (DomainException) -> SeeTransactionsEffect = { error ->
        SeeTransactionsEffect.ShowError(error.toUserMessage())
    }

    init {
        combine(categoryRepository.all(), filter) { categories, current ->
            val activeId = current.categoryIds.firstOrNull()
            if (activeId != null && categories.none { it.categoryId == activeId }) {
                filter.value = current.copy(categoryIds = emptySet())
            }
        }.launchSafeIn(onError = onDomainError)

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
            .launchSafeIn(onError = onDomainError)

        transactionRepository.observeTotals()
            .map<TransactionTotals, Long?> { totals -> totals.movementCount }
            .catch { emit(null) }
            .onEach { count -> updateState { copy(movementCount = count) } }
            .launchSafeIn(onError = onDomainError)

        combine(selectedMonth, filter, ::Pair)
            .debounce { (_, currentFilter) -> if (currentFilter.query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
            .distinctUntilChanged()
            .flatMapLatest { (month, currentFilter) ->
                // Both branches catch their own failures: a top-level .catch would kill the whole
                // collector on the first database error, leaving the month selector dead until the
                // ViewModel is recreated.
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
            .launchSafeIn(onError = onDomainError)

        today
            .map { date -> YearMonth.of(date) }
            .onEach { month ->
                val previousCalendarMonth = calendarMonth
                calendarMonth = month
                updateState { copy(currentMonth = month) }
                if (month != previousCalendarMonth && selectedMonth.value == previousCalendarMonth) selectMonth(month)
            }
            .launchSafeIn(onError = onDomainError)
    }

    override fun onIntent(intent: SeeTransactionsIntent) {
        when (intent) {
            is SeeTransactionsIntent.OnMonthSelected -> {
                selectMonth(intent.month)
                updateState { copy(showMonthPicker = false) }
            }

            is SeeTransactionsIntent.OnQueryChanged -> {
                filter.value = filter.value.copy(query = intent.query)
                updateState { copy(query = intent.query) }
            }

            is SeeTransactionsIntent.OnCategoryToggled -> toggleCategory(CategoryId(intent.categoryId))

            is SeeTransactionsIntent.OnCategorySelected -> {
                val id = CategoryId(intent.categoryId)
                filter.value = filter.value.copy(categoryIds = setOf(id))
            }

            SeeTransactionsIntent.OnClearCategoryFilter -> {
                filter.value = filter.value.copy(categoryIds = emptySet()).withAmountRange(null, null)
                updateState { copy(minAmount = null, maxAmount = null) }
            }

            SeeTransactionsIntent.OnClearFilters -> {
                filter.value = TransactionFilter.None
                updateState { copy(query = "", minAmount = null, maxAmount = null) }
            }

            is SeeTransactionsIntent.AmountFilterIntent -> onAmountFilterIntent(intent)

            is SeeTransactionsIntent.ScreenChromeIntent -> onScreenChromeIntent(intent)
        }
    }

    private fun toggleCategory(id: CategoryId) {
        val next: Set<CategoryId> = if (id in filter.value.categoryIds) emptySet() else setOf(id)
        filter.value = filter.value.copy(categoryIds = next)
    }

    private fun onAmountFilterIntent(intent: SeeTransactionsIntent.AmountFilterIntent) {
        when (intent) {
            is SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetRequested ->
                updateState { copy(amountSheetTarget = intent.target) }

            SeeTransactionsIntent.AmountFilterIntent.OnAmountSheetDismissed ->
                updateState { copy(amountSheetTarget = null) }

            is SeeTransactionsIntent.AmountFilterIntent.OnAmountConfirmed -> onAmountConfirmed(intent)

            is SeeTransactionsIntent.AmountFilterIntent.OnAmountBoundCleared -> {
                val newMin: Money? = if (intent.target == AmountRangeTarget.Min) null else filter.value.minAmount
                val newMax: Money? = if (intent.target == AmountRangeTarget.Max) null else filter.value.maxAmount
                filter.value = filter.value.withAmountRange(newMin, newMax)
                updateState { copy(minAmount = newMin, maxAmount = newMax) }
            }
        }
    }

    private fun onAmountConfirmed(intent: SeeTransactionsIntent.AmountFilterIntent.OnAmountConfirmed) {
        val target: AmountRangeTarget = state.value.amountSheetTarget ?: return
        val amount: Money = centsToMoney(intent.digits)
        val newMin: Money? = if (target == AmountRangeTarget.Min) amount else filter.value.minAmount
        val newMax: Money? = if (target == AmountRangeTarget.Max) amount else filter.value.maxAmount
        filter.value = filter.value.withAmountRange(newMin, newMax)
        updateState {
            copy(
                minAmount = filter.value.minAmount,
                maxAmount = filter.value.maxAmount,
                amountSheetTarget = null,
            )
        }
    }

    private fun onScreenChromeIntent(intent: SeeTransactionsIntent.ScreenChromeIntent) {
        when (intent) {
            SeeTransactionsIntent.ScreenChromeIntent.OnFilterSheetRequested ->
                updateState { copy(showFilterSheet = true) }

            SeeTransactionsIntent.ScreenChromeIntent.OnFilterSheetDismissed ->
                updateState { copy(showFilterSheet = false) }

            SeeTransactionsIntent.ScreenChromeIntent.OnSearchRequested -> updateState { copy(searchRequested = true) }

            SeeTransactionsIntent.ScreenChromeIntent.OnSearchClosed -> {
                filter.value = filter.value.copy(query = "")
                updateState { copy(searchRequested = false, query = "") }
            }

            SeeTransactionsIntent.ScreenChromeIntent.OnMonthPickerRequested ->
                updateState { copy(showMonthPicker = true) }

            SeeTransactionsIntent.ScreenChromeIntent.OnMonthPickerDismissed ->
                updateState { copy(showMonthPicker = false) }
        }
    }

    // Advances the month synchronously so the tap gets instant feedback; the list stream lands the
    // actual rows later.
    private fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
        updateState { copy(month = month) }
    }

    // Name breaks ties so two categories tied at zero usage don't swap places between emissions.
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

// Applies a slice, unless it is a late answer for a month the user has already left — that one
// would pair the new month's label with the old month's rows and totals. Search slices carry no
// month and always apply.
internal fun SeeTransactionsUiState.withListSlice(slice: ListSlice, selectedMonth: YearMonth): SeeTransactionsUiState {
    val isStale = slice.month != null && slice.month != selectedMonth
    return if (isStale) this else copy(days = slice.days, summary = slice.summary)
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
