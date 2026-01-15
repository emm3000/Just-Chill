package com.emm.justchill.hh.category

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emm.domain.category.Category
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryType
import com.emm.justchill.hh.transaction.SelectableCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SelectCategoryViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    var state by mutableStateOf(SelectCategoryUi())
        private set

    init {
        snapshotFlow { state.query }
            .debounce(220L)
            .distinctUntilChanged()
            .flatMapLatest { searchQuery ->
                flow {
                    if (searchQuery.isBlank()) {
                        emit(Pair(state.allIncomes, state.allExpenses))
                        return@flow
                    }
                    val filteredIncomes = state.allIncomes.filter { category ->
                        category.name.contains(searchQuery, ignoreCase = true)
                    }
                    val filteredExpenses = state.allExpenses.filter { category ->
                        category.name.contains(searchQuery, ignoreCase = true)
                    }
                    emit(Pair(filteredIncomes, filteredExpenses))

                }.flowOn(Dispatchers.Default)
            }
            .onEach {
                state = state.copy(
                    filteredIncomes = it.first,
                    filteredExpenses = it.second,
                )
            }
            .launchIn(viewModelScope)
        fetchAll()
    }

    fun fetchAll() = categoryRepository.all()
        .map(::mapToUiAndPartitionByType)
        .onEach {
            state = state.copy(
                allIncomes = it.first,
                allExpenses = it.second,
                filteredIncomes = it.first,
                filteredExpenses = it.second,
            )
        }
        .launchIn(viewModelScope)

    fun updateQuery(newQuery: String) {
        state = state.copy(query = newQuery)
    }
}

private fun mapToUiAndPartitionByType(
    categories: List<Category>,
): Pair<List<SelectableCategory>, List<SelectableCategory>> = categories.map { category ->
    SelectableCategory(
        categoryId = category.categoryId,
        name = category.name,
        icon = AppIconCatalog.findById(category.icon),
        color = findById(category.color),
        categoryType = category.categoryType,
    )
}.filter { selectableCategory ->
    selectableCategory.categoryType != CategoryType.Both
}.partition { selectableCategory ->
    selectableCategory.categoryType == CategoryType.Income
}