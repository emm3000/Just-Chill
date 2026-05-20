package com.emm.justchill.hh.seetransactions

import androidx.lifecycle.viewModelScope
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.SearchTransactionsUseCase
import com.emm.domain.transaction.TransactionFilter
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionWithCategory
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.hh.transaction.TransactionUi
import com.emm.justchill.hh.transaction.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DayGroup(val date: LocalDate, val transactions: List<TransactionUi>) {
    val readableDate: String
        get() {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            return when (date) {
                today -> "HOY"

                yesterday -> "AYER"

                else -> {
                    val formatter = DateTimeFormatter.ofPattern(
                        "MMMM dd",
                        Locale.forLanguageTag("es"),
                    )
                    date.format(formatter).uppercase()
                }
            }
        }
}

private const val TOP_N = 5
private const val SHOW_MORE_THRESHOLD = 8

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SeeTransactionsViewModel(
    private val searchTransactions: SearchTransactionsUseCase,
    categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
) : MviViewModel<SeeTransactionsUiState, SeeTransactionsIntent, SeeTransactionsEffect>() {

    override val initialState = SeeTransactionsUiState()

    private val filter = MutableStateFlow(TransactionFilter.None)

    init {
        // Auto-reset filter if the active category got deleted out from under us.
        combine(categoryRepository.all(), filter) { categories, current ->
            val activeId = current.categoryIds.firstOrNull()
            if (activeId != null && categories.none { it.categoryId == activeId }) {
                filter.value = current.copy(categoryIds = emptySet())
            }
        }.launchIn(viewModelScope)

        // Top chips + overflow + sheet items + counts.
        combine(
            categoryRepository.all(),
            transactionRepository.fetchAllWithCategory(),
            filter,
        ) { categories, txs, current ->
            buildChipsState(categories, txs, current)
        }
            .onEach { chips ->
                updateState {
                    copy(
                        topChips = chips.topChips,
                        overflowCount = chips.overflowCount,
                        activeCategory = chips.activeCategory,
                        sheetItems = chips.sheetItems,
                        incomeCount = chips.incomeCount,
                        spendCount = chips.spendCount,
                    )
                }
            }
            .launchIn(viewModelScope)

        // Filtered + grouped list.
        filter
            .debounce { f -> if (f.query.isBlank()) 0L else 250L }
            .distinctUntilChanged()
            .flatMapLatest(searchTransactions::invoke)
            .map(::groupByDate)
            .map(::mapToDayGroup)
            .catch { emit(emptyList()) }
            .onEach { days -> updateState { copy(days = days) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SeeTransactionsIntent) {
        when (intent) {
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

    private fun buildChipsState(
        categories: List<Category>,
        txs: List<TransactionWithCategory>,
        current: TransactionFilter,
    ): ChipsState {
        val counts: Map<CategoryId, Int> = txs
            .mapNotNull { it.category?.categoryId }
            .groupingBy { it }
            .eachCount()

        val rankedByUsage = categories.sortedByDescending { counts[it.categoryId] ?: 0 }
        val activeId: CategoryId? = current.categoryIds.firstOrNull()

        val showAll = categories.size <= SHOW_MORE_THRESHOLD
        val visibleBase = if (showAll) rankedByUsage else rankedByUsage.take(TOP_N)

        // Always surface the active chip even if it's not in the top.
        val visibleCategories: List<Category> = if (activeId != null &&
            visibleBase.none { it.categoryId == activeId }
        ) {
            val active = categories.firstOrNull { it.categoryId == activeId }
            if (active != null) listOf(active) + visibleBase.dropLast(1) else visibleBase
        } else {
            visibleBase
        }

        val topChips = visibleCategories.map { cat ->
            CategoryChipUi(
                id = cat.categoryId.value,
                name = cat.name,
                colorId = cat.color,
                selected = cat.categoryId == activeId,
            )
        }

        val overflowCount = if (showAll) 0 else categories.size - visibleCategories.size

        val activeInfo = activeId?.let { id ->
            categories.firstOrNull { it.categoryId == id }?.let {
                ActiveCategoryInfo(it.categoryId.value, it.name)
            }
        }

        val sheetItems = categories
            .sortedBy { it.name.lowercase(Locale.forLanguageTag("es")) }
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

        return ChipsState(topChips, overflowCount, activeInfo, sheetItems, incomeCount, spendCount)
    }

    private data class ChipsState(
        val topChips: List<CategoryChipUi>,
        val overflowCount: Int,
        val activeCategory: ActiveCategoryInfo?,
        val sheetItems: List<CategorySheetItem>,
        val incomeCount: Int,
        val spendCount: Int,
    )
}

private fun mapToDayGroup(transactionGroups: Map<LocalDate, List<TransactionWithCategory>>): List<DayGroup> =
    transactionGroups.map { (date, transactions) ->
        DayGroup(date = date, transactions = transactions.toUi())
    }

private fun groupByDate(transactions: List<TransactionWithCategory>): Map<LocalDate, List<TransactionWithCategory>> =
    transactions.groupBy { transaction ->
        Instant.ofEpochMilli(transaction.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
