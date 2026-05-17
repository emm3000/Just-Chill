package com.emm.justchill.hh.seetransactions

import androidx.lifecycle.viewModelScope
import com.emm.domain.category.CategoryRepository
import com.emm.domain.shared.CategoryId
import com.emm.domain.transaction.SearchTransactionsUseCase
import com.emm.domain.transaction.TransactionFilter
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

data class DayGroup(
    val date: LocalDate,
    val transactions: List<TransactionUi>,
) {
    val readableDate: String
        get() {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            return when (date) {
                today -> "HOY"
                yesterday -> "AYER"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern(
                        "MMMM dd", Locale.forLanguageTag("es"),
                    )
                    date.format(formatter).uppercase()
                }
            }
        }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SeeTransactionsViewModel(
    private val searchTransactions: SearchTransactionsUseCase,
    categoryRepository: CategoryRepository,
) : MviViewModel<SeeTransactionsUiState, SeeTransactionsIntent, SeeTransactionsEffect>() {

    override val initialState = SeeTransactionsUiState()

    private val filter = MutableStateFlow(TransactionFilter.None)

    init {
        categoryRepository.all()
            .combine(filter) { categories, currentFilter ->
                categories.map { cat ->
                    CategoryChipUi(
                        id = cat.categoryId.value,
                        name = cat.name,
                        colorId = cat.color,
                        selected = cat.categoryId in currentFilter.categoryIds,
                    )
                }
            }
            .onEach { chips -> updateState { copy(categoryChips = chips) } }
            .launchIn(viewModelScope)

        filter
            .debounce { f -> if (f.query.isBlank()) 0L else 250L }
            .distinctUntilChanged()
            .flatMapLatest(searchTransactions::invoke)
            .map(::groupByDate)
            .map(::mapToDayGroup)
            .catch { emit(emptyList()) }
            .onEach { days ->
                updateState {
                    copy(
                        days = days,
                        isFilterActive = !filter.value.isEmpty,
                    )
                }
            }
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
                val newSet = filter.value.categoryIds.toMutableSet().apply {
                    if (!add(id)) remove(id)
                }
                filter.value = filter.value.copy(categoryIds = newSet)
            }
            SeeTransactionsIntent.OnClearFilters -> {
                filter.value = TransactionFilter.None
                updateState { copy(query = "") }
            }
        }
    }
}

private fun mapToDayGroup(
    transactionGroups: Map<LocalDate, List<TransactionWithCategory>>,
): List<DayGroup> = transactionGroups.map { (date, transactions) ->
    DayGroup(date = date, transactions = transactions.toUi())
}

private fun groupByDate(
    transactions: List<TransactionWithCategory>,
): Map<LocalDate, List<TransactionWithCategory>> = transactions.groupBy { transaction ->
    Instant.ofEpochMilli(transaction.date)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
